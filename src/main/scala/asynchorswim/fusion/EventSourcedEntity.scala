package asynchorswim.fusion

import akka.actor.{ActorLogging, Props, Stash}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import asynchorswim.fusion.ControlMessages.StreamEventEnvelope

import scala.reflect.runtime.{universe => ru}

class EventSourcedEntity[A <: Entity[A] : ru.TypeTag](implicit timeProvider: TimeProvider) extends PersistentActor with ActorLogging with Stash {
  override val persistenceId: String  = context.parent.path.name + "-"  + self.path.name

  private var state: A = Entity.companion[A].empty

  private val ctx = Context(timeProvider, Some(context), log)

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, ss) => state = ss.asInstanceOf[A]
    case e: Event => state = state.applyEvent(e)
    case RecoveryCompleted => unstashAll()
    case _ => stash()
  }

  override def receiveCommand: Receive = {
    case ControlMessages.Stop =>
      context.stop(self)
    case ControlMessages.TakeSnapshot =>
      saveSnapshot(state)
    case ControlMessages.Expunge(_) =>
      deleteMessages(Long.MaxValue)
      context.stop(self)
    case ControlMessages.SetInactivityTimeout(timeout) =>
      context.setReceiveTimeout(timeout)
    case ControlMessages.StreamCommandEnvelope(t, o, m) =>
      val events = (state.receive(ctx) orElse state.unhandled(ctx))(m).to[collection.immutable.Seq]
      if (events.nonEmpty) persistAll[Event](events){ e => state = state.applyEvent(e) }
      sender ! events
                 .map { e => StreamEventEnvelope(t, o, e) }
                 .collect { case see @ StreamEventEnvelope(_, _, e: Externalized) => see } ++ Seq(ControlMessages.StreamEventEnvelope(t, o, ControlMessages.CommandComplete))
    case _: ControlMessage =>
    case msg =>
      val events = (state.receive(ctx) orElse state.unhandled(ctx))(msg).to[collection.immutable.Seq]
      if (events.nonEmpty) persistAll[Event](events){ e => state = state.applyEvent(e) }
  }
}

object EventSourcedEntity extends EntityPropsFactory {
  override def props[A <: Entity[A]: ru.TypeTag](implicit timeProvider: TimeProvider): Props = Props(new EventSourcedEntity[A])
}

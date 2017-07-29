package asynchorswim.fusion

import akka.actor.{ActorLogging, Props, Stash}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import ControlMessages.StreamEventEnvelope

import scala.reflect.runtime.{universe => ru}

class CommandSourcedEntity[A <: Entity[A] : ru.TypeTag](implicit fc: FusionConfig) extends PersistentActor with ActorLogging with Stash {
  override val persistenceId: String  = context.parent.path.name + "-"  + self.path.name

  private var state: A = Entity.companion[A].empty

  private val ctx = Context(fc.timeProvider, Some(context), log)

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, ss) =>
      state = ss.asInstanceOf[A]
    case RecoveryCompleted =>
      unstashAll()
    case CommandEnvelope(c) =>
      processCommand(c)
    case m =>
      stash()
  }

  override def receiveCommand: Receive = {
    case ControlMessages.Stop =>
      context.stop(self)
    case ControlMessages.Expunge(_) =>
      deleteMessages(Long.MaxValue)
      context.stop(self)
    case ControlMessages.SetInactivityTimeout(timeout) =>
      context.setReceiveTimeout(timeout)
    case ControlMessages.TakeSnapshot =>
      saveSnapshot(state)
    case ControlMessages.StreamCommandEnvelope(t, o, c: Command) =>
      if (fc.asyncIO)
        persistAsync(CommandEnvelope(c)) { pc =>
          sender ! (state.receive(ctx) orElse state.unhandled(ctx)) (pc)
            .map { e => state = state.applyEvent(e); StreamEventEnvelope(t, o, e) }
            .collect { case see@StreamEventEnvelope(_, _, e: Externalized) => see } ++ Seq(ControlMessages.StreamEventEnvelope(t, o, ControlMessages.CommandComplete))
        }
      else
        persist(CommandEnvelope(c)) { pc =>
          sender ! (state.receive(ctx) orElse state.unhandled(ctx)) (pc)
            .map { e => state = state.applyEvent(e); StreamEventEnvelope(t, o, e) }
            .collect { case see@StreamEventEnvelope(_, _, e: Externalized) => see } ++ Seq(ControlMessages.StreamEventEnvelope(t, o, ControlMessages.CommandComplete))
        }
    case ControlMessages.StreamCommandEnvelope(t, o, m) =>
      sender ! (state.receive(ctx) orElse state.unhandled(ctx))(m)
        .map { e => state = state.applyEvent(e); StreamEventEnvelope(t, o, e) }
        .collect { case see@StreamEventEnvelope(_, _, e: Externalized) => see } ++ Seq(ControlMessages.StreamEventEnvelope(t, o, ControlMessages.CommandComplete))
    case _: ControlMessage =>
    case c: Command =>
      if (fc.asyncIO)
        persistAsync(CommandEnvelope(c)) { ce => processCommand(ce.c) }
      else
        persist(CommandEnvelope(c)) { ce => processCommand(ce.c) }
    case msg =>
      processCommand(msg)

  }

  private def processCommand(c: Any): Unit =
    (state.receive(ctx) orElse state.unhandled(ctx))(c) foreach { e =>  state = state.applyEvent(e) }
}

object CommandSourcedEntity extends EntityPropsFactory {
  override def props[A <: Entity[A]: ru.TypeTag](implicit fc: FusionConfig): Props = Props(new CommandSourcedEntity[A])
}

case class CommandEnvelope(c: Command)

package asynchorswim.fusion

import akka.actor.{Actor, ActorLogging, Props}
import asynchorswim.fusion.ControlMessages.StreamEventEnvelope

import scala.reflect.runtime.{universe => ru}

class PersistentEntity[A <: Entity[A] with Persistable : ru.TypeTag](persistor: Persistor[A])(implicit fc: FusionConfig) extends Actor with ActorLogging {
  private val persistenceId: String = context.parent.path.name + "-" + self.path.name

  private var state: A = persistor.get(self.path.name).getOrElse(Entity.companion[A].empty)

  private val ctx = Context(fc.timeProvider, Some(context), log)

  override def receive: Receive = {
    case ControlMessages.Stop =>
      context.stop(self)
    case ControlMessages.Expunge(_) =>
      persistor.remove(persistenceId)
      context.stop(self)
    case ControlMessages.SetInactivityTimeout(timeout)  =>
      context.setReceiveTimeout(timeout)
    case ControlMessages.StreamCommandEnvelope(t, o, m) =>
      val retVal = (state.receive(ctx) orElse state.unhandled(ctx))(m)
        .map { e =>  if (!e.isInstanceOf[Informational]) { state = state.applyEvent(e) }; StreamEventEnvelope(t, o, e) }
        .collect { case see @ StreamEventEnvelope(_, _, e: Externalized) => see } ++ Seq(ControlMessages.StreamEventEnvelope(t, o, ControlMessages.CommandComplete))
      if (retVal.nonEmpty) persistor.put(persistenceId, state)
      sender ! retVal
    case _: ControlMessage =>
    case msg =>
      val events = (state.receive(ctx) orElse state.unhandled(ctx))(msg)
      events foreach { e =>  if (!e.isInstanceOf[Informational]) { state = state.applyEvent(e) } }
      if (events.nonEmpty) persistor.put(persistenceId, state)
  }
}

class PersistentEntityPropsFactory[A <: Entity[A] with Persistable : ru.TypeTag](persistor: Persistor[A]) extends EntityPropsFactory {
  override def props[B <: Entity[B] : ru.TypeTag](implicit fc: FusionConfig): Props = Props(new PersistentEntity[A](persistor))
}

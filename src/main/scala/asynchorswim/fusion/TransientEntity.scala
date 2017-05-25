package asynchorswim.fusion

import akka.actor.{Actor, ActorLogging, Props}
import asynchorswim.fusion.ControlMessages.StreamEventEnvelope

import scala.reflect.runtime.{universe => ru}

class TransientEntity[A <: Entity[A] : ru.TypeTag](implicit timeProvider: TimeProvider) extends Actor with ActorLogging {
  private var state: A = Entity.companion[A].empty

  private val ctx = Context(timeProvider, Some(context), log)

  override def receive: Receive = {
    case ControlMessages.Stop | ControlMessages.Expunge(_) =>
      context.stop(self)
    case ControlMessages.StreamCommandEnvelope(t, o, m) =>
      sender ! (state.receive(ctx) orElse state.unhandled(ctx))(m)
        .map { e =>  state = state.applyEvent(e); StreamEventEnvelope(t, o, e) }
        .collect { case see @ StreamEventEnvelope(_, _, e: Externalized) => see } ++ Seq(StreamEventEnvelope(t, o, ControlMessages.CommandComplete))
    case ControlMessages.SetInactivityTimeout(timeout) =>
      context.setReceiveTimeout(timeout)
    case _: ControlMessage =>
    case msg =>
      (state.receive(ctx) orElse state.unhandled(ctx))(msg) foreach { e =>  state = state.applyEvent(e) }
  }
}

object TransientEntity extends EntityPropsFactory {
  override def props[A <: Entity[A]: ru.TypeTag](implicit timeProvider: TimeProvider): Props = Props(new TransientEntity[A])
}

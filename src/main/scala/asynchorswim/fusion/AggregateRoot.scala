package asynchorswim.fusion

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}

class AggregateRoot[A <: Entity[A] : ru.TypeTag](epf: EntityPropsFactory)(implicit timeProvider: TimeProvider) extends Actor with ActorLogging {
  private val entities = new mutable.AnyRefMap[String, ActorRef]

  override def receive: Receive = {
    case ControlMessages.ShardingEnvelope(id, msg) =>
      entities.getOrElseUpdate(id, context.actorOf(epf.props[A], id)) forward msg
    case ControlMessages.Stop =>
      broadcast(ControlMessages.Stop)
      context.stop(self)
    case sat: ControlMessages.SetInactivityTimeout =>
      broadcast(sat)
    case e @ ControlMessages.Expunge(id) =>
      entities.get(id) foreach (_ forward e)
      entities.remove(id)
    case sce @ ControlMessages.StreamCommandEnvelope(_, _, msg: ShardingId) =>
      entities.getOrElseUpdate(msg.id, context.actorOf(epf.props[A], msg.id)) forward sce
    case msg: ShardingId =>
      entities.getOrElseUpdate(msg.id, context.actorOf(epf.props[A], msg.id)) forward msg
    case cm: ControlMessage =>
      broadcast(cm)
    case _ =>
  }

  private def broadcast(msg: Any) = entities.values foreach { _ ! msg }
}

object AggregateRoot {
  def props[A <: Entity[A] : ru.TypeTag](epf: EntityPropsFactory)(implicit timeProvider: TimeProvider): Props = Props(new AggregateRoot[A](epf))
}

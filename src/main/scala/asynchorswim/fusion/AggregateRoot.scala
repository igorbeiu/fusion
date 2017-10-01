package asynchorswim.fusion

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.reflect.runtime.{universe => ru}

class AggregateRoot[A <: Entity[A] : ru.TypeTag](epf: EntityPropsFactory)(implicit fc: FusionConfig) extends Actor with ActorLogging {
  private val entities = new mutable.AnyRefMap[String, ActorRef]
  private var actorTTL: Option[Duration] = None

  override def receive: Receive = {
    case ControlMessages.ShardingEnvelope(id, msg) =>
      entities.getOrElseUpdate(id, context.actorOf(epf.props[A], id)) forward msg
    case ControlMessages.Stop =>
      broadcast(ControlMessages.Stop)
      context.stop(self)
    case ControlMessages.SetInactivityTimeout(d) =>
      actorTTL = Option(d)
    case e @ ControlMessages.Expunge(id) =>
      entities.get(id) foreach (_ forward e)
      entities.remove(id)
    case sce @ ControlMessages.StreamCommandEnvelope(_, _, msg: ShardingId) =>
      sendToChild(msg.id, sce)
    case msg: ShardingId =>
      sendToChild(msg.id, msg)
    case Terminated(aRef) =>
      entities.find(kv => kv._2 == aRef) foreach { e => entities.remove(e._1) }
    case cm: ControlMessage =>
      broadcast(cm)
    case _ =>
  }

  private def sendToChild(id: String, msg: Any) = {
    val isNew = entities.isDefinedAt(id)
    val target = entities.getOrElseUpdate(id, context.actorOf(epf.props[A], id))
    if (isNew) {
      context watch target
      actorTTL foreach { d => target ! ControlMessages.SetInactivityTimeout(d) }
    }
    target forward msg
  }

  private def broadcast(msg: Any) = entities.values foreach { _ ! msg }
}

object AggregateRoot {
  def props[A <: Entity[A] : ru.TypeTag](epf: EntityPropsFactory)(implicit fc: FusionConfig): Props = Props(new AggregateRoot[A](epf))
}

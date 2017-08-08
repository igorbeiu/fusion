package asynchorswim.fusion

import scala.concurrent.duration.Duration

sealed trait ControlMessage

object ControlMessages {
  final case object Stop extends ControlMessage
  final case object TakeSnapshot extends ControlMessage
  final case class ShardingEnvelope(id: String, payload: Any) extends ControlMessage
  final case class SetInactivityTimeout(d: Duration)
  final case class Expunge(id: String) extends ControlMessage with ShardingId
  final case class StreamCommandEnvelope[A](topic: String, offset: A, message: Any)
  final case class StreamEventEnvelope[A](topic: String, offset: A, event: Event)

  case object CommandComplete extends ControlMessage with Event with Informational
}

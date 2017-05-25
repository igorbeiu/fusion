package asynchorswim.fusion

import akka.actor.ActorContext
import akka.event.LoggingAdapter

case class  Context(timeProvider: TimeProvider, actorContext: Option[ActorContext], log: LoggingAdapter)

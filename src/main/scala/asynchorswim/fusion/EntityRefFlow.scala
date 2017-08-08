package asynchorswim.fusion

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import asynchorswim.fusion.ControlMessages._

class EntityRefFlow(target: ActorRef, parallelizm: Int = 1, maintainOrder: Boolean = true)(implicit timeout: Timeout) {
  val flow =
    if (maintainOrder) 
      Flow[StreamCommandEnvelope[_]]
        .mapAsync[Seq[StreamEventEnvelope[_]]](parallelizm)(c => (target ? c).mapTo[Seq[StreamEventEnvelope[_]]])
        .mapConcat[StreamEventEnvelope[_]](_.toList)
    else
      Flow[StreamCommandEnvelope[_]]
        .mapAsyncUnordered[Seq[StreamEventEnvelope[_]]](parallelizm)(c => (target ? c).mapTo[Seq[StreamEventEnvelope[_]]])
        .mapConcat[StreamEventEnvelope[_]](_.toList)
}

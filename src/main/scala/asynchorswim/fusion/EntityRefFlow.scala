package asynchorswim.fusion

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import asynchorswim.fusion.ControlMessages._

class EntityRefFlow(target: ActorRef)(implicit timeout: Timeout) {
  val flow = Flow[StreamCommandEnvelope[_]]
               .mapAsync[Seq[StreamEventEnvelope[_]]](1)(c => (target ? c).mapTo[Seq[StreamEventEnvelope[_]]])
               .mapConcat[StreamEventEnvelope[_]](_.toList)
}

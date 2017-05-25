package asynchorswim.aurora

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.util.Timeout

import scala.concurrent.ExecutionContext

trait AkkaSupport { this: ConfigSupport =>
  implicit val system: ActorSystem = ActorSystem(config.getString("akka.actorSystemName"), config)
  implicit val ec: ExecutionContext = system.dispatcher
  val decider: Supervision.Decider = { _ => Supervision.Resume }
  implicit val mat: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(decider))
  implicit val timeout: Timeout = Timeout(config.getDuration("akka.timeout"))
  implicit val log = Logging(system.eventStream, "Main")
}

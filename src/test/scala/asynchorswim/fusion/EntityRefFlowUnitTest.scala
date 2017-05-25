package asynchorswim.fusion

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import asynchorswim.fusion.ControlMessages.{CommandComplete, StreamCommandEnvelope, StreamEventEnvelope}
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.language.postfixOps

class EntityRefFlowUnitTest extends AsyncFlatSpec with Matchers {

  private implicit val system = ActorSystem()
  private implicit val mat = ActorMaterializer()
  private implicit val ec = system.dispatcher
  private implicit val timeout = Timeout(1 second)
  private val now = Instant.now
  private implicit val timeProvider = new FixedTimeProvider(now)

  private val testEntity = system.actorOf(TransientEntity.props[TestEntity], "testEntity")

  "EntityRefFlow" should "return externalized events as expected" in {
    Source(immutable.Seq("inc", "inc", "dec")
             .zipWithIndex
             .map { case (c, i) => StreamCommandEnvelope("test", i, c) })
      .via(new EntityRefFlow(testEntity).flow)
      .runWith(Sink.seq) map { s =>
        s shouldBe Seq(StreamEventEnvelope("test",0,Incremented),
                       StreamEventEnvelope("test",0,CommandComplete),
                       StreamEventEnvelope("test",1,Incremented),
                       StreamEventEnvelope("test",1,CommandComplete),
                       StreamEventEnvelope("test",2,Decremented),
                       StreamEventEnvelope("test",2,CommandComplete))
      }
  }
}

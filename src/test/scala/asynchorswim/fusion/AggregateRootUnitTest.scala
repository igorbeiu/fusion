package asynchorswim.fusion

import java.time.Instant

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

class AggregateRootUnitTest extends AsyncFlatSpec with Matchers {
  private val now = Instant.now
  private implicit val fc = FusionConfig(new FixedTimeProvider(now), asyncIO = false)
  private implicit val timeout = Timeout(1 second)
  private val system = ActorSystem()

  private val sut = system.actorOf(AggregateRoot.props[TestEntity](TransientEntity), "testAggregate")

  "AggregateRoot" should "create entities correctly" in {
    sut ! Increment("0")
    sut ! Increment("0")
    sut ! Decrement("1")
    (sut ? Query("0")) map { v => v shouldBe 2}
    (sut ? Query("1")) map { v => v shouldBe -1}
    (sut ? Query("88")) map { v => v shouldBe 0}
  }

  it should "handle stream commands correctly" in {
    (sut ? ControlMessages.StreamCommandEnvelope("topic1", 2L, Increment("0"))) map {s => s shouldBe Seq(ControlMessages.StreamEventEnvelope("topic1", 2L, Incremented)) }
    (sut ? Query("0")) map { v => v shouldBe 3}
    (sut ? Query("1")) map { v => v shouldBe -1}
    (sut ? Query("88")) map { v => v shouldBe 0}
  }

  it should "handle Expunging of actors" in {
    sut ! ControlMessages.Expunge("0")
    Thread.sleep(500)
    (sut ? Query("0")) map { v => v shouldBe 0}
  }
}

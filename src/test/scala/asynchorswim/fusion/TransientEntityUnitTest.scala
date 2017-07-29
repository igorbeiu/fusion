package asynchorswim.fusion

import java.time.Instant

import akka.actor.ActorSystem
import org.scalatest.{AsyncFlatSpec, Matchers}
import akka.pattern.ask
import akka.util.Timeout
import ControlMessages.{StreamCommandEnvelope, StreamEventEnvelope}

import concurrent.duration._
import language.postfixOps

class TransientEntityUnitTest extends AsyncFlatSpec with Matchers {
  private val system = ActorSystem()
  private val now = Instant.now
  private implicit val fc = FusionConfig(new FixedTimeProvider(now), asyncIO = false)
  private val sut = system.actorOf(TransientEntity.props[TestEntity], "testEntity")
  private implicit val timeout = Timeout(1 second)

  "TransientEntity" should "handle normal messages" in {
    sut ! "inc"
    sut ! "inc"
    (sut ? "value") map { _ shouldBe 2 }
    sut ! "dec"
    (sut ? "value") map { _ shouldBe 1 }
  }

  it should "respond to StreamCommandEnvelopes with StreamEventEnvelopes" in {
    (sut ? StreamCommandEnvelope("topic", 123L, "inc")) map { _ shouldBe Seq(StreamEventEnvelope("topic", 123L, Incremented)) }
    (sut ? "value") map { _ shouldBe 2 }
  }

  it should "ignore unknown commands" in {
    (sut ? StreamCommandEnvelope("topic", 123L, "xxx")) map { _ shouldBe Seq.empty }
    (sut ? "value") map { _ shouldBe 2 }
  }

  it should "not persist state" in {
    sut ! ControlMessages.Stop
    Thread.sleep(500)
    val sut2 =  system.actorOf(TransientEntity.props[TestEntity], "testEntity")
    (sut2 ? "value") map { _ shouldBe 0 }
  }
}


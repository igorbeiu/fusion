package asynchorswim.fusion

import java.time.Instant

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import asynchorswim.fusion.ControlMessages.{StreamCommandEnvelope, StreamEventEnvelope}
import com.typesafe.config.ConfigFactory
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

class EventSourcedEntityUnitTest extends AsyncFlatSpec with Matchers {

  private val configString =
    s"""
       |akka {
       |  persistence {
       |    journal.plugin = "inmemory-journal"
       |    snapshot-store.plugin = "inmemory-snapshot-store"
       |  }
       |}
       |inmemory-journal {
       |  circuit-breaker {
       |    max-failures = 10
       |    call-timeout = 600s
       |    reset-timeout = 30s
       |  }
       |}
       |inmemory-read-journal {
       |  refresh-interval = "100ms"
       |  offset-mode = "sequence"
       |  write-plugin = "inmemory-journal"
       |  max-buffer-size = "5000"
       |}
     """.stripMargin
  private val config = ConfigFactory.parseString(configString)
  private val system = ActorSystem("testSystem", config)
  private val now = Instant.now
  private implicit val fc = FusionConfig(new FixedTimeProvider(now), asyncIO = false)
  private val sut = system.actorOf(EventSourcedEntity.props[TestEntity], "testEntity")
  private implicit val timeout = Timeout(10 second)

  "EventSourcedEntity" should "handle normal messages" in {
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

  it should "retain state across lifetimes" in {
    sut ! ControlMessages.Stop
    Thread.sleep(1000)
    val sut2 = system.actorOf(EventSourcedEntity.props[TestEntity], "testEntity")
    (sut2 ? "value") map {v =>  sut2 ! ControlMessages.Stop ;v shouldBe 2 }
  }

  it should "delete its state after an Expunge message" in {
    Thread.sleep(1000)
    val sut2 = system.actorOf(EventSourcedEntity.props[TestEntity], "testEntity")
    (sut2 ? "value") map { _ shouldBe 2 }
    sut2 ! ControlMessages.Expunge("")
    Thread.sleep(1000)
    val sut3 = system.actorOf(EventSourcedEntity.props[TestEntity], "testEntity")
    (sut3 ? "value") map { _ shouldBe 0 }
    sut3 ! "inc"
    (sut3 ? "value") map { _ shouldBe 1 }
    sut3 ! ControlMessages.Stop
    Thread.sleep(1000)
    val sut4 = system.actorOf(EventSourcedEntity.props[TestEntity], "testEntity")
    (sut4 ? "value") map { _ shouldBe 1 }

  }

}

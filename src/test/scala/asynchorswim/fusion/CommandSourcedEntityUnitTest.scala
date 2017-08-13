package asynchorswim.fusion

import java.time.Instant

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import ControlMessages.{StreamCommandEnvelope, StreamEventEnvelope}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}
import akka.pattern.ask

import concurrent.duration._
import language.postfixOps

class CommandSourcedEntityUnitTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

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
  private val sut = system.actorOf(CommandSourcedEntity.props[TestEntity], "testEntity")
  private implicit val timeout = Timeout(10 second)

  "CommandSourcedEntity" should "handle normal messages" in {
    sut ! "inc"
    sut ! "inc"
    (sut ? "value") map { _ shouldBe 2 }
    sut ! "dec"
    (sut ? "value") map { _ shouldBe 1 }
    sut ! Inc
    sut ! Inc
    sut ! Dec
    (sut ? "value") map { _ shouldBe 2 }
  }

  it should "respond to StreamCommandEnvelopes with StreamEventEnvelopes" in {
    (sut ? StreamCommandEnvelope("topic", 123L, "inc")) map { _ shouldBe Seq(StreamEventEnvelope("topic", 123L, Incremented)) }
    (sut ? "value") map { _ shouldBe 3 }
  }

  it should "ignore unknown commands" in {
    (sut ? StreamCommandEnvelope("topic", 123L, "xxx")) map { _ shouldBe Seq.empty }
    (sut ? "value") map { _ shouldBe 3 }
  }

  it should "retain state across lifetimes" in {
    sut ! ControlMessages.Stop
    Thread.sleep(100)
    val sut2 = system.actorOf(CommandSourcedEntity.props[TestEntity], "testEntity")
    (sut2 ? "value") map { v => sut2 ! ControlMessages.Stop; v shouldBe 1 }
  }

  it should "handle Expunge correctly" in {
    Thread.sleep(100)
    val sut2 = system.actorOf(CommandSourcedEntity.props[TestEntity], "testEntity")
    (sut2 ? "value") map { _ shouldBe 1 }
    sut2 ! ControlMessages.Expunge("")
    Thread.sleep(100)
    val sut3 = system.actorOf(CommandSourcedEntity.props[TestEntity], "testEntity")
    (sut3 ? "value") map { _ shouldBe 0 }
  }

  override def afterAll(): Unit = system.terminate()
}


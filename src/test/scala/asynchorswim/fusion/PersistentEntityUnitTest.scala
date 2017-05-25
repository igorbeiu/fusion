package asynchorswim.fusion

import java.time.Instant

import akka.actor.ActorSystem
import akka.util.Timeout
import org.scalatest.{AsyncFlatSpec, Matchers}
import akka.pattern.ask
import ControlMessages.{StreamCommandEnvelope, StreamEventEnvelope}

import concurrent.duration._
import language.postfixOps
import scala.collection.mutable

class PersistentEntityUnitTest extends AsyncFlatSpec with Matchers {
  private val now = Instant.now
  private implicit val timeProvider = new FixedTimeProvider(now)
  private implicit val system = ActorSystem()
  private implicit val timeout = Timeout(10 seconds)
  private val sepf = new PersistentEntityPropsFactory[TestEntity](new TestEntityTestPersistor)
  private val sut = system.actorOf(sepf.props, "testEntity")

  "PersistentEntity" should "handle normal messages" in {
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
    Thread.sleep(100)
    val sut2 = system.actorOf(sepf.props, "testEntity")
    (sut2 ? "value") map {v =>  sut2 ! ControlMessages.Stop ;v shouldBe 2 }
  }

  it should "delete its state after an Expunge message" in {
    val sut2 = system.actorOf(sepf.props, "testEntity")
    (sut2 ? "value") map { _ shouldBe 2 }
    sut2 ! ControlMessages.Expunge("")
    Thread.sleep(500)
    val sut3 = system.actorOf(sepf.props, "testEntity")
    (sut3 ? "value") map { _ shouldBe 0 }
    sut3 ! "inc"
    (sut3 ? "value") map { _ shouldBe 1 }
    sut3 ! ControlMessages.Stop
    Thread.sleep(500)
    val sut4 = system.actorOf(sepf.props, "testEntity")
    (sut4 ? "value") map { _ shouldBe 1 }
  }
}

class TestEntityTestPersistor extends Persistor[TestEntity] {
  val cache = new mutable.AnyRefMap[String, TestEntity]

  def get(id: String): Option[TestEntity] = cache.get(id)
  def put(id: String, value: TestEntity): Unit = cache.put(id, value)
  def remove(id: String): Unit = cache.remove(id)
}

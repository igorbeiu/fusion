package asynchorswim.fusion

import java.time.Instant

import akka.event.LoggingAdapter
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class EntityUnitTest extends FlatSpec with Matchers with MockitoSugar {

  private val now = Instant.now
  private val ctx = Context(new FixedTimeProvider(now), None, mock[LoggingAdapter])
  private val sut = TestEntity.empty

  "Entity" should "respond to commands with events" in {
    sut.receive(ctx)("inc") shouldBe Seq(Incremented, Ignored)
    sut.receive(ctx)("dec") shouldBe Seq(Decremented)
  }

  it should "respond with an empty sequence to unknown commands" in {
    sut.unhandled(ctx)(1234) shouldBe Seq.empty
  }

  it should "dynamically return its companion object" in {
    Entity.companion[TestEntity].empty shouldBe TestEntity.empty
  }

}

case class TestEntity(num: Int) extends Entity[TestEntity] with Persistable {

  override def id = "count"

  override def receive(implicit ctx: Context): PartialFunction[Any, Seq[Event]] = {
    case "inc" | Increment(_) | Inc => applying(Incremented, Ignored)
    case "dec" | Decrement(_) | Dec => applying(Decremented)
    case "value" | Query(_) => reply(num); NoOp
  }
  override def applyEvent: PartialFunction[Event, TestEntity] = {
    case Incremented => copy(num = num + 1)
    case Decremented => copy(num = num - 1)
    case Ignored => this
  }
}

object TestEntity extends EntityCompanion[TestEntity] {
  val empty = TestEntity(0)
}

case object Inc extends Command
case object Dec extends Command
case class Increment(id: String) extends ShardingId
case class Decrement(id: String) extends ShardingId
case class Query(id: String) extends ShardingId
case object Incremented extends Event with Externalized
case object Decremented extends Event with Externalized
case object Ignored extends Event


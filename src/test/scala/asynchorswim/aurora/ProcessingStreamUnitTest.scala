package asynchorswim.aurora

import java.time.Instant

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import asynchorswim.fusion.ControlMessages.StreamCommandEnvelope
import asynchorswim.fusion._
import org.scalatest.concurrent.Eventually._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.language.postfixOps

class ProcessingStreamUnitTest extends FlatSpec with Matchers {

  private val now = Instant.now()
  implicit val timeProvider = new FixedTimeProvider(now)
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val akkaTimeout = Timeout(10.seconds)
  private val cc = system.actorOf(TransientEntity.props[CommandCounter], "commandCounter")
  private val bm = system.actorOf(Props[NullActor], "bookmarks")

  "ProcessingStream" should "process streams" in {
    var c = 0

    val s = Source(
      immutable.Seq(StreamCommandEnvelope("test", 0, "Test"),
      StreamCommandEnvelope("test", 1, "Test"),
      StreamCommandEnvelope("test", 2, "Test"),
      StreamCommandEnvelope("test", 3, "Test"))
    )

    val sut = system.actorOf(ProcessingStream.props(s, cc, bm, { case _ => c = c + 1 }))
    eventually(timeout(5 seconds), interval(500 millis)) { c shouldBe 8 }
  }
}

final case class CommandCounter(count: Int) extends Entity[CommandCounter] {
  def receive(implicit context: Context) = {
    case Query => reply(count); NoOp
    case _ => applying(Incremented)
  }
  def applyEvent = {
    case Incremented => copy(count = count + 1)
  }
}


object CommandCounter extends EntityCompanion[CommandCounter] {
  val empty = CommandCounter(0)

  case object Query

  case object Incremented extends Event with Externalized

}

class NullActor extends Actor {
 def receive = {
   case _ =>
 }
}

package asynchorswim.fusion.cluster

import java.time.Instant

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}
import akka.pattern.ask
import asynchorswim.fusion._

import concurrent.duration._
import language.postfixOps

class ClusterAggregateRootUnitTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {
  private val configString =
    s"""
       |akka {
       |  actor {
       |    provider = "cluster"
       |  }
       |  remote {
       |    artery {
       |      enabled = on
       |      canonical.hostname = "127.0.0.1"
       |      canonical.port = 25520
       |    }
       |  }
       |  persistence {
       |    journal.plugin = "inmemory-journal"
       |    snapshot-store.plugin = "inmemory-snapshot-store"
       |  }
       |  cluster {
       |    seed-nodes = ["akka://ClusterAggregateRootUnitTestSystem@127.0.0.1:25520"]
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
  private val system = ActorSystem("ClusterAggregateRootUnitTestSystem", config)
  private val now = Instant.now
  private implicit val fc = FusionConfig(new FixedTimeProvider(now))
  private implicit val timeout = Timeout(10 second)
  private val sut = system.actorOf(ClusterAggregateRoot.props[ClusterWordCount](TransientEntity, "ClusterWordCount"), "clusterWords")

  "ClusterAggregateRoot" should "create entities correctly" in {
    "The rain in Spain falls mainly in the plain"
      .split(" ")
      .map(_.toUpperCase()) foreach { w => sut ! ClusterWordCount.Count(w) }
    (sut ? ClusterWordCount.Query("THE")) map { _ shouldBe 2 }
    (sut ? ClusterWordCount.Query("SPAIN")) map { _ shouldBe 1 }
    (sut ? ClusterWordCount.Query("XXX")) map { _ shouldBe 0 }
  }

  it should "handle Expunge correctly" in {
    sut ! ControlMessages.Expunge("THE")
    Thread.sleep(500)
    (sut ? ClusterWordCount.Query("THE")) map { _ shouldBe 0 }
  }

  override def afterAll(): Unit = system.terminate()

}

case class ClusterWordCount(count: Int) extends Entity[ClusterWordCount] {

  override def receive(implicit ctx: Context): PartialFunction[Any, Seq[Event]] = {
    case ClusterWordCount.Count(w) => applying(ClusterWordCount.Increment)
    case ClusterWordCount.Query(w) => reply(count); NoOp
  }

  override def applyEvent: PartialFunction[Event, ClusterWordCount] = {
    case ClusterWordCount.Increment => copy( count = count + 1)
  }
}

object ClusterWordCount extends EntityCompanion[ClusterWordCount] {
  val empty = ClusterWordCount(0)

  case class Count(id: String) extends ShardingId with Command
  case class Query(id: String) extends ShardingId


  case object Increment extends Event
}
package ops.plasma.fusion.cluster

import java.time.Instant

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import ops.plasma.fusion._
import ops.plasma.fusion.cluster.WordCount.{Count, Increment, Query}
import org.scalatest.{AsyncFlatSpec, Matchers}
import akka.pattern.ask
import asynchorswim.fusion
import asynchorswim.fusion._

import concurrent.duration._
import language.postfixOps

class ClusterAggregateRootUnitTest extends AsyncFlatSpec with Matchers {
  private val configString =
    s"""
       |akka {
       |  actor {
       |    provider = "cluster"
       |  }
       |  remote {
       |    log-remote-lifecycle-events = off
       |    netty.tcp {
       |      hostname = "127.0.0.1"
       |      port = 2552
       |    }
       |  }
       |  persistence {
       |    journal.plugin = "inmemory-journal"
       |    snapshot-store.plugin = "inmemory-snapshot-store"
       |  }
       |  cluster {
       |    seed-nodes = ["akka.tcp://TestSystem@127.0.0.1:2552"]
       |    sharding {
       |      guardian-name = sharding
       |      role = ""
       |      remember-entities = off
       |      coordinator-failure-backoff = 5 s
       |      retry-interval = 2 s
       |      buffer-size = 100000
       |      handoff-timeout = 60 s
       |      shard-start-timeout = 10 s
       |      shard-failure-backoff = 10 s
       |      entity-restart-backoff = 10 s
       |      rebalance-interval = 10 s
       |      journal-plugin-id = ""
       |      snapshot-plugin-id = ""
       |      state-store-mode = "ddata"
       |      snapshot-after = 1000
       |      keep-nr-of-batches = 2
       |      least-shard-allocation-strategy {
       |        rebalance-threshold = 10
       |        max-simultaneous-rebalance = 3
       |      }
       |      waiting-for-state-timeout = 5 s
       |      updating-state-timeout = 5 s
       |      entity-recovery-strategy = "all"
       |      entity-recovery-constant-rate-strategy {
       |        frequency = 100 ms
       |        number-of-entities = 5
       |      }
       |      distributed-data {
       |        majority-min-cap = 5
       |        durable.keys = ["shard-*"]
       |        akka.cluster.sharding.distributed-data.max-delta-elements = 5
       |      }
       |    }
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
  private val system = ActorSystem("TestSystem", config)
  private val now = Instant.now
  private implicit val timeProvider = new FixedTimeProvider(now)
  private implicit val timeout = Timeout(10 second)
  private val sut = system.actorOf(ClusterAggregateRoot.props[WordCount](TransientEntity, "WordCount"), "Words")

  "ClusterAggregateRoot" should "create entities correctly" in {
    "The rain in Spain falls mainly in the plain"
      .split(" ")
      .map(_.toUpperCase()) foreach { w => sut ! Count(w) }
    (sut ? Query("THE")) map { _ shouldBe 2 }
    (sut ? Query("SPAIN")) map { _ shouldBe 1 }
    (sut ? Query("XXX")) map { _ shouldBe 0 }
  }

  it should "handle Expunge correctly" in {
    sut ! ControlMessages.Expunge("THE")
    Thread.sleep(500)
    (sut ? Query("THE")) map { _ shouldBe 0 }
  }

}

case class WordCount(count: Int) extends Entity[WordCount] {

  override def receive(implicit ctx: Context): PartialFunction[Any, Seq[Event]] = {
    case Count(w) => applying(Increment)
    case Query(w) => reply(count); NoOp
  }

  override def applyEvent: PartialFunction[Event, WordCount] = {
    case Increment => copy( count = count + 1)
  }
}

object WordCount extends fusion.EntityCompanion[WordCount] {
  val empty = WordCount(0)

  case class Count(id: String) extends ShardingId with Command
  case class Query(id: String) extends ShardingId


  case object Increment extends Event
}
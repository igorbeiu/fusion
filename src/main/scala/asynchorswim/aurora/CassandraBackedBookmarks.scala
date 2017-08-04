package asynchorswim.aurora

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.typesafe.config.Config
import io.getquill.{CassandraAsyncContext, SnakeCase}
import scala.collection.mutable
import scala.concurrent.Await

class CassandraBackedBookmarks(config: Config, appName: Symbol, default: String)(implicit timeout: Timeout) extends Actor with ActorLogging {

  private val bm = new mutable.AnyRefMap[String, String]
  private lazy val ctx = new CassandraAsyncContext[SnakeCase](appName.name)
  import ctx._

  private implicit val ec = context.dispatcher

  override def preStart(): Unit = {
    val a = quote {
      query[BookmarkEntry].filter(_.app == lift(appName.name))
    }
    Await.result(ctx.run(a).recover { case _ => List.empty } , timeout.duration) foreach { bme => bm.put(bme.topic, bme.value) }
  }

  def receive: Receive = {
    case Bookmarks.Get(k) =>
      sender ! Bookmarks.BookmarkValue(bm.getOrElse(k, default))
    case Bookmarks.Put(k, v) =>
      val bme = BookmarkEntry(appName.name, k, v.toString)
      val a = quote {
        query[BookmarkEntry].insert(lift(bme))
      }
      ctx.run(a)
  }
}

object CassandraBackedBookmarks {
  def props(config: Config, appName: Symbol, default: String)(implicit timeout: Timeout): Props = Props(new CassandraBackedBookmarks(config, appName, default)) 
}

final case class BookmarkEntry(app: String, topic: String, value: String)

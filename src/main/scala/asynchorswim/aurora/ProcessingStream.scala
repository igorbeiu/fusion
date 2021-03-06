package asynchorswim.aurora

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import asynchorswim.fusion.{ControlMessages, EntityRefFlow, Event}
import asynchorswim.fusion.ControlMessages.{StreamCommandEnvelope, StreamEventEnvelope}

class ProcessingStream(in: Source[StreamCommandEnvelope[_], NotUsed], domain: ActorRef, bookmarks: ActorRef, notify: PartialFunction[Event, Unit], metrics: StreamMetrics, parallelizm: Int = 1, maintainOrder: Boolean = true)
  (implicit val mat: ActorMaterializer, timrout: Timeout) extends Actor with ActorLogging {

  private val stream =
    in
      .log("StreamCommands", sce => sce)
      .map { c => metrics.in.increment(); c}
      .via(new EntityRefFlow(domain, parallelizm, maintainOrder).flow)
      .map { case see@StreamEventEnvelope(_, _, e) =>
        metrics.events.increment()
        (notify orElse ProcessingStream.ignore) (e)
        if (e == ControlMessages.CommandComplete) metrics.complete.increment()
        see
      }
      .map { case see@StreamEventEnvelope(t, o, _) => bookmarks ! Bookmarks.Put(t, o); see }
      .runWith(Sink.ignore)

  def receive: Receive = {
    case ControlMessages.Stop => stream.failed
    case _ =>
  }
}

object ProcessingStream {

  val ignore: PartialFunction[Event, Unit] = { case evt: Event => }

  def props(in: Source[StreamCommandEnvelope[_], NotUsed], domain: ActorRef, bookmarks: ActorRef, notify: PartialFunction[Event, Unit], metrics: StreamMetrics = StreamMetrics.empty, parallelizm: Int = 1, maintainOrder: Boolean = true)(implicit mat: ActorMaterializer, timrout: Timeout): Props =
    Props(new ProcessingStream(in, domain, bookmarks, notify, metrics, parallelizm, maintainOrder))
}
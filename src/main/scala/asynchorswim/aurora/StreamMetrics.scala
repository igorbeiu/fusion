package asynchorswim.aurora

import kamon.metric.instrument._

case class StreamMetrics(in: Counter, error: Counter, complete: Counter, events: Counter)

object StreamMetrics {
  val empty = StreamMetrics(NullCounter, NullCounter, NullCounter, NullCounter)
}

object NullCounter extends Counter {
  override def cleanup: Unit = {}
  override def collect(context: CollectionContext): SnapshotType = CounterSnapshot(0)
  override def increment(times: Long): Unit = {}
  override def increment(): Unit = {}
}


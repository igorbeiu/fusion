package ops.plasma.fusion.util

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import asynchorswim.fusion.Externalized

class ExternalizedEventAdapter extends WriteEventAdapter {
  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case e: Externalized => Tagged(e, Set(ExternalizedEventAdapter.Tag))
    case e => e
  }
}

object ExternalizedEventAdapter {
  val Tag = "externalized"
}

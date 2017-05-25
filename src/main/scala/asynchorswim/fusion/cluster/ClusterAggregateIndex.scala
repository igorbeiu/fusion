package asynchorswim.fusion.cluster

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ddata.{DistributedData, Key, ORMultiMap, ORMultiMapKey}
import akka.cluster.ddata.Replicator._
import asynchorswim.fusion.util.AggregateIndex._

class ClusterAggregateIndex extends Actor with ActorLogging {
  private val mapKey = ORMultiMapKey[String, String](context.self.path.name)
  private val writePolicy = WriteLocal
  private val readPolicy = ReadLocal

  private val ValueKey = "__VALUE__"
  private val MapKey = "__MAP__"

  private val replicator = DistributedData(context.system).replicator
  private implicit val node = Cluster(context.system)

  def receive: Receive = {
    case Add(key, value) =>
      replicator ! Update(mapKey, ORMultiMap.empty[String, String], writePolicy)(_.addBinding(key, value))
    case Remove(key, value) =>
      replicator ! Update(mapKey, ORMultiMap.empty[String, String], writePolicy)(_.removeBinding(key, value))

    case QueryKey(key) =>
      replicator ! Get(mapKey, readPolicy, Option((key, sender())))
    case QueryValues =>
      replicator ! Get(mapKey, readPolicy, Option((ValueKey, sender())))
    case QueryMap =>
      replicator ! Get(mapKey, readPolicy, Option((MapKey, sender())))

    case g@GetSuccess(k: Key[ORMultiMap[String, String]]@unchecked, Some((MapKey, replyTo: ActorRef))) =>
      replyTo ! g.get[ORMultiMap[String, String]](k).entries
    case g@GetSuccess(k: Key[ORMultiMap[String, String]]@unchecked, Some((ValueKey, replyTo: ActorRef))) =>
      replyTo ! g.get[ORMultiMap[String, String]](k).entries.values.flatten.toSet
    case g@GetSuccess(k: Key[ORMultiMap[String, String]]@unchecked, Some((key: String, replyTo: ActorRef))) =>
      replyTo ! g.get[ORMultiMap[String, String]](k).get(key).getOrElse(Set.empty[String])

    case u: UpdateSuccess[_] =>

    case msg =>
      log.warning("Unhandled message - {}", msg)
  }
}

object ClusterAggregateIndex {
  def props: Props = Props(new ClusterAggregateIndex)
}
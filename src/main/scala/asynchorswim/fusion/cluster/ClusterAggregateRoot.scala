package asynchorswim.fusion.cluster

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import asynchorswim.fusion._
import asynchorswim.fusion.ControlMessages.StreamCommandEnvelope
import ops.plasma.fusion._

import reflect.runtime.{universe => ru}

class ClusterAggregateRoot[A <: Entity[A] : ru.TypeTag](entityPropsFactory: EntityPropsFactory, typeName: String,  proxyOnly: Boolean = false)(implicit fc: FusionConfig)
  extends Actor with ActorLogging {

  private val shardRegion: ActorRef =
    if (!proxyOnly)
      ClusterSharding(context.system).start(
        typeName = typeName,
        entityProps = entityPropsFactory.props[A],
        settings = ClusterShardingSettings(context.system),
        extractEntityId = {
          case msg: ShardingId => (msg.id, msg)
          case msg @ StreamCommandEnvelope(_, _, c: ShardingId)  => (c.id, msg)
        },
        extractShardId =  {
          case msg: ShardingId => (msg.id.hashCode % 255).toString
          case msg @ StreamCommandEnvelope(_, _, c: ShardingId)  => (c.id.hashCode % 255).toString
        }
      )
    else
      ClusterSharding(context.system).startProxy(
        typeName = typeName,
        role = None,
        extractEntityId = {
          case ControlMessages.ShardingEnvelope(id, m) => (id, m)
          case msg: ShardingId => (msg.id, msg)
          case msg @ StreamCommandEnvelope(_, _, c: ShardingId)  => (c.id, msg)
        },
        extractShardId =  {
          case ControlMessages.ShardingEnvelope(id, m) => (id.hashCode % 255).toString
          case msg: ShardingId => (msg.id.hashCode % 255).toString
          case msg @ StreamCommandEnvelope(_, _, c: ShardingId)  => (c.id.hashCode % 255).toString
        }
      )

  override def receive: Receive = {
    case msg: ControlMessages.ShardingEnvelope => shardRegion forward msg
    case msg: ShardingId => shardRegion forward msg
    case msg @ StreamCommandEnvelope(_, _, m: ShardingId) => shardRegion forward msg
    case msg => log.warning("Invalid shard message received : {}", msg)
  }
}

object ClusterAggregateRoot {
  def props[A <: Entity[A] : ru.TypeTag](epf: EntityPropsFactory, typeName: String, proxyOnly: Boolean = false)(implicit fc: FusionConfig): Props =
    Props(new ClusterAggregateRoot[A](epf, typeName, proxyOnly))
}
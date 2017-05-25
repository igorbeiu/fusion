package  asynchorswim.fusion.util

import asynchorswim.fusion.util.AggregateIndexEntity.{EntryAdded, EntryRemoved, KeyRemoved}
import asynchorswim.fusion.{Context, Entity, EntityCompanion, Event}


object AggregateIndex {
  final case class Add(key: String, value: String)
  final case class Remove(key: String, value: String)
  final case class RemoveKey(key: String)
  final case class QueryKey(key: String)
  final case object QueryKeys
  final case object QueryValues
  final case object QueryMap
}

final case class AggregateIndexEntity(index: Map[String, Set[String]]) extends Entity[AggregateIndexEntity] {
  override def receive(implicit ctx: Context): PartialFunction[Any, Seq[Event]] = {
    case AggregateIndex.Add(key, value) if !index.getOrElse(key, Set.empty).contains(value) =>
      applying(EntryAdded(key, value))
    case AggregateIndex.Add(_, _) =>
      NoOp
    case AggregateIndex.QueryKey(key) =>
      reply(index.getOrElse(key, Set.empty))
      NoOp
    case AggregateIndex.Remove(key, value) if index.getOrElse(key, Set.empty).contains(value) =>
      applying(EntryRemoved(key, value))
    case AggregateIndex.Remove(_, _) =>
      NoOp
    case AggregateIndex.RemoveKey(key) if index.keySet.contains(key) =>
      applying(KeyRemoved(key))
    case AggregateIndex.RemoveKey(_) =>
      NoOp
    case AggregateIndex.QueryKeys =>
      reply(index.keySet)
      NoOp
    case AggregateIndex.QueryValues =>
      reply(index.values.flatten)
      NoOp
    case AggregateIndex.QueryMap =>
      reply(index)
      NoOp
  }

  override def applyEvent: PartialFunction[Event, AggregateIndexEntity] = {
    case EntryAdded(key, value) =>
      copy(index = index.updated(key, index.getOrElse(key, Set.empty) + value))
    case EntryRemoved(key, value) =>
      copy(index = index.updated(key, index.getOrElse(key, Set.empty) - value))
    case KeyRemoved(key) =>
      copy(index = index - key)
  }
}

object AggregateIndexEntity extends EntityCompanion[AggregateIndexEntity] {
  final val empty = AggregateIndexEntity(Map.empty[String, Set[String]])

  final case class EntryAdded(key: String, value: String) extends Event
  final case class EntryRemoved(key: String, value: String) extends Event
  final case class KeyRemoved(key: String) extends Event
}
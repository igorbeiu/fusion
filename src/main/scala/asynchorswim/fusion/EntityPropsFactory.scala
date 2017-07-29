package asynchorswim.fusion

import akka.actor.Props

import scala.reflect.runtime.{universe => ru}

trait EntityPropsFactory {
  def props[A <: Entity[A]: ru.TypeTag](implicit fc: FusionConfig): Props
}

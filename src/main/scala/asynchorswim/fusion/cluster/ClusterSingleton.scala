package asynchorswim.fusion.cluster

import akka.actor.{ActorSystem, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import asynchorswim.fusion.ControlMessages

object ClusterSingleton {
  def props(sp: Props)(implicit system: ActorSystem): Props =
    ClusterSingletonManager.props(
      singletonProps = sp,
      terminationMessage = ControlMessages.Stop,
      settings = ClusterSingletonManagerSettings(system)
    )
}

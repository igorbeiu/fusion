package asynchorswim.aurora

import akka.cluster.Cluster
import collection.JavaConverters._

trait ClusterSupport { this: ConfigSupport with AkkaSupport =>
  val cluster = Cluster(system)
  val nodeRoles: Set[String] = config.getStringList("akka.cluster.roles").asScala.toSet

  def isInRole(r: String): Boolean = nodeRoles.contains(r)
}  

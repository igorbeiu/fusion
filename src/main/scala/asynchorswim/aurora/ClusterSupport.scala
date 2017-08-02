import akka.cluster.Cluster
import collection.JavaConverters._

trait ClusterSupport { this: AkkaSupport =>
  val cluster = Cluster(system)
  val nodeRoles: Set[String] = Config.getStringList("akka.cluster.roles").asScala.toSet

  def isInRole(r: String): Boolean = nodeRoles.contains(r)
}  

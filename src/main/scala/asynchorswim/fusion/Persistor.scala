package asynchorswim.fusion

trait Persistable {
  def id: String
}

trait Persistor[A <: Persistable] {
  def get(id: String): Option[A]
  def put(id: String, value: A): Unit
  def remove(id: String): Unit
}
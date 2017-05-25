package asynchorswim.fusion

trait Persistor[A] {
  def get(id: String): Option[A]
  def put(id: String, value: A): Unit
  def remove(id: String): Unit
}

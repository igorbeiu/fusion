package asynchorswim.fusion.util

object Bookmarks {
  final case class Get(topic: String)
  final case class Put[A](topic: String, offset: A)
}


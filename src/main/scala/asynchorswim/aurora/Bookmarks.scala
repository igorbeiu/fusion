package asynchorswim.aurora

object Bookmarks {
  final case class Get(topic: String)
  final case class Put[A](topic: String, offset: A)
  
  final case class BookmarkValue[A](value: A)
}


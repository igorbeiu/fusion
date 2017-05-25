package asynchorswim.fusion

trait Command

trait Event

trait Externalized

trait ShardingId {
  def id: String
}

trait Entity[A <: Entity[A]] {
  def receive(implicit ctx: Context): PartialFunction[Any, Seq[Event]]
  def applyEvent: PartialFunction[Event, A]

  def unhandled(implicit ctx: Context): PartialFunction[Any, Seq[Event]] = { case _ => NoOp }

  @inline final protected def applying(events: Event*): Seq[Event] = Seq(events:_*)
  @inline final protected def reply(msg: Any)(implicit ctx: Context): Unit = ctx.actorContext foreach { ac => ac.sender() ! msg }
  @inline final protected def NoOp: Seq[Event] = Seq.empty
}

trait EntityCompanion[A <: Entity[A]] {
  val empty: A
}

object Entity {
  import scala.reflect.runtime.{universe => ru}
  private lazy val universeMirror = ru.runtimeMirror(getClass.getClassLoader)

  def companion[A <: Entity[A]](implicit ttt: ru.TypeTag[A]): EntityCompanion[A] = {
      val companionMirror = universeMirror.reflectModule(ru.typeOf[A].typeSymbol.companion.asModule)
      companionMirror.instance.asInstanceOf[EntityCompanion[A]]
  }

}
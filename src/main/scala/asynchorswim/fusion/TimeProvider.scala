package asynchorswim.fusion

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}

import scala.language.postfixOps

trait TimeProvider {
  def now: Instant
  def localDate: LocalDate
  def millis: Long
}

object SystemTimeProvider extends TimeProvider {
  override def now: Instant = Instant.now()

  override def localDate: LocalDate = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toLocalDate

  override def millis: Long = Instant.now().toEpochMilli
}

class FixedTimeProvider(val now: Instant) extends TimeProvider {
  override def localDate: LocalDate = LocalDateTime.ofInstant(now, ZoneOffset.UTC).toLocalDate
  override def millis: Long = now.toEpochMilli
}

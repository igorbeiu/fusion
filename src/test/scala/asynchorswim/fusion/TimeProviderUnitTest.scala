package asynchorswim.fusion

import java.time.{Instant, LocalDateTime, ZoneOffset}

import org.scalatest.{FlatSpec, Matchers}

class TimeProviderUnitTest extends FlatSpec with Matchers {
  val now = Instant.now
  val fixed = new FixedTimeProvider(now)

  "FixedTimeProvider" should "return the same instant from its creation" in {
    fixed.now shouldBe now
  }

  it should "return the correct LocalDate" in {
    fixed.localDate shouldBe LocalDateTime.ofInstant(now, ZoneOffset.UTC).toLocalDate
  }

  it should "return the correct millisecond value" in {
    fixed.millis shouldBe now.toEpochMilli
  }

  "SystemTimeProvider" should "return current milliseconds" in {
    SystemTimeProvider.millis shouldBe System.currentTimeMillis() +- 100
  }

  it should "return the current LocalDate" in {
    SystemTimeProvider.localDate shouldBe LocalDateTime.ofInstant(now, ZoneOffset.UTC).toLocalDate
  }

  it should "return the current Instant" in {
    SystemTimeProvider.now.toEpochMilli shouldBe System.currentTimeMillis() +- 100
  }

}

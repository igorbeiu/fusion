package asynchorswim.aurora

import org.scalatest.{FlatSpec, Matchers}

class ConfigSupportUnitTest extends FlatSpec with Matchers {

  "ConfigSupport" should "make config values available to classes derived from App" in {
    val sut = new Test
    sut.foo shouldBe "bar"
  }
}

class Test extends App with ConfigSupport {
  def name = 'TESTAPP

  def foo = config.getString("foo")
}
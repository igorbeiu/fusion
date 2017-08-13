package asynchorswim.aurora

import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import concurrent.duration._

class AkkaSupportUnitTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  object TestApp extends App with ConfigSupport with AkkaSupport {
    def name = 'TESTAPP

    def shutdown = system.terminate()
  }

  "AkkaSupport" should "provide Akka declarations when used as a mixin" in {
    TestApp.timeout shouldBe Timeout(10.seconds)
  }

  override def afterAll(): Unit = TestApp.shutdown
}

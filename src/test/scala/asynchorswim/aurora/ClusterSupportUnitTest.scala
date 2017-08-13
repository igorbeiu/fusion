package asynchorswim.aurora

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class ClusterSupportUnitTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  object Sut extends App with ConfigSupport with AkkaSupport with ClusterSupport {
    def name = 'TESTAPP

    def shutdown = system.terminate()
  }

  "ClusterSupport" should "provide access to cluster node role(s)" in {
    Sut.nodeRoles.contains("foo") shouldBe true
    Sut.nodeRoles.contains("bar") shouldBe true
    Sut.nodeRoles.contains("baz") shouldBe false
  }

  override def afterAll(): Unit = Sut.shutdown
}

package asynchorswim.aurora

import org.json4s.jackson.Serialization
import org.json4s.{Formats, ShortTypeHints}
import org.scalatest.{FlatSpec, Matchers}

class JsonMessageProtocolUnitTest extends FlatSpec with Matchers {

  private val protocol = new TestProtocol

  "JsonMessageProtocol" should "Serialize messages correctly" in {
    val msg = TestMsg("test test test")
    protocol.toJson(msg) shouldBe """{"jsonClass":"TestMsg","id":"test test test"}"""
    // protocol.fromJson(protocol.toJson(msg)) shouldBe msg
  }

}

sealed trait TestMessage extends Message
case class TestMsg(id: String) extends TestMessage
case class TestMsg2(n: Int) extends TestMessage

class TestProtocol extends JsonMessageProtocol[TestMessage] {
  override implicit val format: Formats = Serialization.formats(ShortTypeHints(List(
    classOf[TestMsg],
    classOf[TestMsg2]
  )))
}
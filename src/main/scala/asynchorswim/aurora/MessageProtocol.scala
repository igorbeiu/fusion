package asynchorswim.aurora

import org.json4s._
import org.json4s.jackson.Serialization.{read, write}

import scala.util.Try

trait Message

trait MessageProtocol[A <: Message] {
  def encode(msg: A): Try[Array[Byte]]
  def decode(buff: Array[Byte]): Try[A]
}

trait JsonMessageProtocol[A <: Message] extends MessageProtocol[A] {
  implicit val format: Formats
  def toJson(msg: A): String = write(msg)
  def fromJson(json: String) : A = read(json)

  override def encode(msg: A): Try[Array[Byte]] = Try(toJson(msg).getBytes)
  override def decode(buff: Array[Byte]): Try[A] = Try(fromJson(new String(buff)))
}
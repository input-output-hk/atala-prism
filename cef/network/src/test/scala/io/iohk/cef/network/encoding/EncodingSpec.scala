package io.iohk.cef.network.encoding

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class EncodingSpec extends FlatSpec {

  implicit val anEncoder = new Encoder[String, Int] {
    override def encode(t: String): Int = Map("a" -> 1)(t)
  }

  implicit val aDecoder = new Decoder[Int, String] {
    override def decode(i: Int): String = Map(1 -> "a")(i)
  }

  val anotherEncoder = new Encoder[Int, Char] {
    override def encode(t: Int): Char = Map(1 -> 'a')(t)
  }

  val anotherDecoder = new Decoder[Char, Int] {
    override def decode(t: Char): Int = Map('a' -> 1)(t)
  }



  "encode" should "accept a pluggable encoder" in {
    encode("a") shouldBe 1
  }

  it should "handle propagate encoding errors" in {
    an[EncodingException] should be thrownBy encode("b")
  }

  "Encoders" should "compose" in {
    encode("a")(anEncoder andThen anotherEncoder) shouldBe 'a'
  }

  "Decoders" should "compose" in {
    decode('a')(anotherDecoder andThen aDecoder) shouldBe "a"
  }

  "decode" should "accept a pluggable decoder" in {
    decode(1) shouldBe "a"
  }

  it should "handle decoding errors" in {
    a[DecodingException] should be thrownBy decode(2)
  }

}

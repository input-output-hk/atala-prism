package io.iohk.cef.encoding

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class EncodingSpec extends FlatSpec {

  implicit val anEncoder: Encoder[String, Int] = (t: String) => Map("a" -> 1)(t)

  implicit val aDecoder: Decoder[Int, String] = (i: Int) => Map(1 -> "a").get(i)

  val anotherEncoder: Encoder[Int, Char] = (t: Int) => Map(1 -> 'a')(t)

  val anotherDecoder: Decoder[Char, Int] = (t: Char) => Map('a' -> 1).get(t)

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
    decode('a')(anotherDecoder andThen aDecoder) shouldBe Some("a")
  }

  "decode" should "accept a pluggable decoder" in {
    decode(1) shouldBe Some("a")
  }

  it should "handle decoding errors" in {
    a[DecodingException] should be thrownBy decode(2)(_ => ???)
  }
}

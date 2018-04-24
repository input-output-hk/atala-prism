package io.iohk.cef.encoding

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class EncodingSpec extends FlatSpec {

  implicit val anEncoder = new Encoder[String, Int] {
    override def encode(t: String): Int = Map("a" -> 1)(t)
  }

  implicit val aDecoder = new Decoder[Int, String] {
    override def decode(i: Int): String = Map(1 -> "a")(i)
  }

  "encode" should "accept a pluggable encoder" in {
    encode("a") shouldBe 1
  }

  it should "handle encoding errors" in {
    pending
  }

  "decode" should "accept a pluggable decoder" in {
    decode(1) shouldBe "a"
  }

  it should "handle decoding errors" in {
    pending
  }

}

package io.iohk.cef.network.encoding.nio
import java.nio.ByteBuffer

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class NativeCodecsSpec extends FlatSpec {

  behavior of "Codecs"

  they should "encode and decode native types" in {
    encodeDecodeTest[Boolean]
    encodeDecodeTest[Byte]
    encodeDecodeTest[Short]
    encodeDecodeTest[Int]
    encodeDecodeTest[Long]
    encodeDecodeTest[Float]
    encodeDecodeTest[Double]
    encodeDecodeTest[Char]
    encodeDecodeTest[String]
  }

  they should "correctly set the buffer position after encoding and decoding" in {
    bufferPositionTest[Boolean]
    bufferPositionTest[Byte]
    bufferPositionTest[Short]
    bufferPositionTest[Int]
    bufferPositionTest[Long]
    bufferPositionTest[Float]
    bufferPositionTest[Double]
    bufferPositionTest[Char]
    bufferPositionTest[String]
  }

  def encodeDecodeTest[T](implicit encoder: NioEncoder[T], decoder: NioDecoder[T], a: Arbitrary[T]): Unit = {
    forAll(arbitrary[T]) {
      t => decoder.decode(encoder.encode(t)) shouldBe Some(t)
    }
  }

  def bufferPositionTest[T](implicit encoder: NioEncoder[T],
                            decoder: NioDecoder[T],
                            a: Arbitrary[T],
                            byteLength: ByteLength[T]): Unit = {
    forAll(arbitrary[T]) { t =>
      val b: ByteBuffer = encoder.encode(t)
      b.position() shouldBe 0
      decoder.decode(b)
      b.position() shouldBe byteLength(t)
    }
  }

  they should "not decode a value for another type" in pending

}

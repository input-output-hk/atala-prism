package io.iohk.cef.network.encoding.nio
import java.nio.ByteBuffer

import io.iohk.cef.network.transport.tcp.NetUtils.randomBytes
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.Inside._
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

  they should "correctly determine length for variable length types" in {
    variableLengthTest[String]
    variableLengthTest[List[String]]
    variableLengthTest[Array[Int]]
    variableLengthTest[List[Int]]
  }

  they should "not decode a value for another type" in {
    mistypeTest[Array[Int], Array[Boolean]]
  }

  they should "return None for an unfully populated buffer" in {
    unfulBufferTest[Array[Int]]
    unfulBufferTest[List[Int]]
  }

  def encodeDecodeTest[T](implicit encoder: NioEncoder[T], decoder: NioDecoder[T], a: Arbitrary[T]): Unit = {
    forAll(arbitrary[T]) { t => decoder.decode(encoder.encode(t)) shouldBe Some(t)
    }
  }

  def mistypeTest[T, U](implicit encoder: NioEncoder[T],
                        decoder: NioDecoder[U],
                        a: Arbitrary[T]): Unit = {

    forAll(arbitrary[T]) { t =>
      val buff: ByteBuffer = encoder.encode(t)
      val dec: Option[U] = decoder.decode(buff)
      dec shouldBe None
      buff.position() shouldBe 0
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

  def variableLengthTest[T](implicit encoder: NioEncoder[T], decoder: NioDecoder[T], a: Arbitrary[T]): Unit = {
    forAll(arbitrary[T]) { t =>
      // create a buffer with one half full of real data
      // and the second half full of rubbish.
      // decoders should not be fooled by this.
      val b: ByteBuffer = encoder.encode(t)
      val newB = ByteBuffer.allocate(b.capacity() * 2).put(b).put(randomBytes(b.capacity()))
      newB.flip()

      inside(decoder.decode(newB)) {
        case Some(tt) => tt shouldBe t
      }
    }
  }

  def unfulBufferTest[T](implicit decoder: NioDecoder[T]): Unit = {
    decoder.decode(ByteBuffer.allocate(0)) shouldBe None
  }
}

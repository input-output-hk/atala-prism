package io.iohk.cef.codecs.nio
package test.utils
import java.nio.ByteBuffer

import io.iohk.cef.test._
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.Inside._
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalactic.Equivalence
import org.scalatest.OptionValues._
import auto._

trait CodecTestingHelpers {

  case class UnexpectedThing()
  object UnexpectedThing {
    implicit val UnexpectedThingArbitrary: Arbitrary[UnexpectedThing] =
      Arbitrary(arbitrary[Unit].map(_ => UnexpectedThing()))
  }

  def encodeDecodeTest[T](
      implicit encoder: NioEncoder[T],
      decoder: NioDecoder[T],
      a: Arbitrary[T],
      eq: Equivalence[T]
  ): Unit = {
    forAll(arbitrary[T]) { t =>
      val e = encoder.encode(t)
      decoder.decode(e).value should equal(t)
    }
  }

  def mistypeTest[T, U](implicit encoder: NioEncoder[T], decoder: NioDecoder[U], a: Arbitrary[T]): Unit = {

    forAll(arbitrary[T]) { t =>
      val buff: ByteBuffer = encoder.encode(t)
      (buff: java.nio.Buffer).position() shouldBe 0
      val dec: Option[U] = decoder.decode(buff)
      dec shouldBe None
      (buff: java.nio.Buffer).position() shouldBe 0
    }
  }

  def bufferPositionTest[T](implicit encoder: NioEncoder[T], decoder: NioDecoder[T], a: Arbitrary[T]): Unit = {
    forAll(arbitrary[T]) { t =>
      val b: ByteBuffer = encoder.encode(t)
      val remaining = b.remaining()
      (b: java.nio.Buffer).position() shouldBe 0
      decoder.decode(b).value should equal(t)
      (b: java.nio.Buffer).position() shouldBe remaining
    }
  }

  def variableLengthTest[T](implicit encoder: NioEncoder[T], decoder: NioDecoder[T], a: Arbitrary[T]): Unit = {
    forAll(arbitrary[T]) { t =>
      // create a buffer with one half full of real data
      // and the second half full of rubbish.
      // decoders should not be fooled by this.
      val b: ByteBuffer = encoder.encode(t)
      val newB = ByteBuffer.allocate(b.capacity() * 2).put(b).put(randomBytes(b.capacity()))
      (newB: java.nio.Buffer).flip()

      inside(decoder.decode(newB)) {
        case Some(tt) => tt shouldBe t
      }
    }
  }

  def unfulBufferTest[T](implicit decoder: NioDecoder[T]): Unit = {

    decoder.decode(ByteBuffer.allocate(0)) shouldBe None

    decoder.decode(ByteBuffer.allocate(5)) shouldBe None

    val b = ByteBuffer.allocate(5)
    b.put(-1.toByte)
    b.put(-12.toByte)
    b.put(-1.toByte)
    b.put(-128.toByte)
    b.put(-118.toByte)
    (b: java.nio.Buffer).position(0)
    decoder.decode(b) shouldBe None
  }

  def testFull[T: NioCodec: Arbitrary]: Unit = {
    encodeDecodeTest[T]
    bufferPositionTest[T]
    variableLengthTest[T]
    unfulBufferTest[T]
    mistypeTest[T, UnexpectedThing]
    mistypeTest[UnexpectedThing, T]
  }
  def testWhenNotEncodingType[T: NioCodec: Arbitrary]: Unit = {
    encodeDecodeTest[T]
    bufferPositionTest[T]
    variableLengthTest[T]
    unfulBufferTest[T]
  }
}

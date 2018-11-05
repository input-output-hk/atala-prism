package io.iohk.cef.codecs.nio
import java.nio.ByteBuffer

import io.iohk.cef.network.transport.tcp.NetUtils.randomBytes
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.Inside._
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalactic.Equivalence
import org.scalatest.OptionValues._

trait CodecTestingHelpers {

  case class UnexpectedThing()
  object UnexpectedThing {
    implicit val UnexpectedThingEncDec: NioEncDec[UnexpectedThing] = {
      import io.iohk.cef.codecs.nio.auto._
      val e: NioEncoder[UnexpectedThing] = genericEncoder
      val d: NioDecoder[UnexpectedThing] = genericDecoder
      NioEncDec(e, d)
    }
    implicit val UnexpectedThingArbitrary: Arbitrary[UnexpectedThing] =
      Arbitrary(arbitrary[Unit].map(_ => UnexpectedThing()))
  }

  def encodeDecodeTest[T](
      implicit encoder: NioEncoder[T],
      decoder: NioDecoder[T],
      a: Arbitrary[T],
      eq: Equivalence[T]): Unit = {
    forAll(arbitrary[T]) { t =>
      val e = encoder.encode(t)
      decoder.decode(e).value should equal(t)
    }
  }

  def mistypeTest[T, U](implicit encoder: NioEncoder[T], decoder: NioDecoder[U], a: Arbitrary[T]): Unit = {

    forAll(arbitrary[T]) { t =>
      val buff: ByteBuffer = encoder.encode(t)
      buff.position() shouldBe 0
      val dec: Option[U] = decoder.decode(buff)
      dec shouldBe None
      buff.position() shouldBe 0
    }
  }

  def bufferPositionTest[T](implicit encoder: NioEncoder[T], decoder: NioDecoder[T], a: Arbitrary[T]): Unit = {
    forAll(arbitrary[T]) { t =>
      val b: ByteBuffer = encoder.encode(t)
      val remaining = b.remaining()
      b.position() shouldBe 0
      decoder.decode(b).value should equal(t)
      b.position() shouldBe remaining
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

    decoder.decode(ByteBuffer.allocate(5)) shouldBe None

    val b = ByteBuffer.allocate(5)
    b.put(-1.toByte)
    b.put(-12.toByte)
    b.put(-1.toByte)
    b.put(-128.toByte)
    b.put(-118.toByte)
    b.position(0)
    decoder.decode(b) shouldBe None
  }

  def testFull[T: NioEncDec: Arbitrary]: Unit = {
    encodeDecodeTest[T]
    bufferPositionTest[T]
    variableLengthTest[T]
    unfulBufferTest[T]
    mistypeTest[T, UnexpectedThing]
    mistypeTest[UnexpectedThing, T]
  }
  def testWhenNotEncodingType[T: NioEncDec: Arbitrary]: Unit = {
    encodeDecodeTest[T]
    bufferPositionTest[T]
    variableLengthTest[T]
    unfulBufferTest[T]
  }
}

package io.iohk.cef.network.encoding.nio
import java.nio.ByteBuffer

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ProductCodecsSpec extends FlatSpec {

  behavior of "ProductCodecs"

  object UserCode {
    case class A(i: Int, b: Boolean, s: String)

    case class B(i: Int)
  }

  import UserCode._

  val as: Gen[A] = for {
    i <- arbitrary[Int]
    b <- arbitrary[Boolean]
    s <- arbitrary[String]
  } yield A(i, b, s)

  they should "encode and decode a user case class" in {
    forAll(as) { a =>
      val buffer = NioEncoder[A].encode(a)
      val maybeA = NioDecoder[A].decode(buffer)
      maybeA shouldBe Some(a)
    }
  }

  they should "not attempt to decode instances of the wrong class" in {
    forAll(as) { a =>
      val buffer = NioEncoder[A].encode(a)

      NioDecoder[B].decode(buffer) shouldBe None
      buffer.position() shouldBe 0
    }
  }

  they should "not attempt to decode incomplete buffers" in {
    forAll(as) { a =>
      val buffer = NioEncoder[A].encode(a)
      val size = buffer.remaining()
      val incompleteBuffer: ByteBuffer =
        ByteBuffer.allocate(size - 1).put(buffer.array(), 0, size - 1).flip().asInstanceOf[ByteBuffer]

      NioDecoder[A].decode(incompleteBuffer) shouldBe None
      incompleteBuffer.position() shouldBe 0
    }
  }
}

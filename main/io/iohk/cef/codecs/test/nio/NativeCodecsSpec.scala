package io.iohk.cef.codecs.nio

import akka.util.ByteString
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import io.iohk.cef.codecs.nio.auto._
import org.scalatest.FlatSpec
import io.iohk.cef.codecs.nio.test.utils._

class NativeCodecsSpec extends FlatSpec with CodecTestingHelpers {

  implicit val arbByteString: Arbitrary[ByteString] = Arbitrary(arbitrary[Array[Byte]].map(arr => ByteString(arr)))

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
    encodeDecodeTest[Array[Int]]
    encodeDecodeTest[Array[Short]]
    encodeDecodeTest[Array[Byte]]
    encodeDecodeTest[Array[Boolean]]
    encodeDecodeTest[ByteString]
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
    bufferPositionTest[Array[Int]]
    bufferPositionTest[ByteString]
  }

  they should "correctly determine length for variable length types" in {
    variableLengthTest[String]
    variableLengthTest[List[String]]
    variableLengthTest[Array[Int]]
    variableLengthTest[List[Int]]
    variableLengthTest[Seq[String]]
    variableLengthTest[ByteString]
  }

  they should "not decode a value for another type" in {
    mistypeTest[Array[Int], Array[Boolean]]
    mistypeTest[Array[Boolean], Array[Int]]
    mistypeTest[Array[Int], ByteString]
    mistypeTest[ByteString, Array[Int]]
  }

  they should "return None for an unfully populated buffer" in {
    unfulBufferTest[Array[Int]]
    unfulBufferTest[List[Int]]
    unfulBufferTest[ByteString]
  }
}

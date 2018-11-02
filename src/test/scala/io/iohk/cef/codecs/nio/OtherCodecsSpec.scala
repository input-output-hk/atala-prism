package io.iohk.cef.codecs.nio
import io.iohk.cef.codecs.nio.auto._
import akka.util.ByteString
import java.nio.ByteBuffer
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import java.util.UUID
import java.time.Instant
import org.scalatest.FlatSpec

class OtherCodecsSpec extends FlatSpec with CodecTestingHelpers {

  implicit val arbByteString: Arbitrary[ByteString] = Arbitrary(arbitrary[Array[Byte]].map(arr => ByteString(arr)))
  implicit val arbByteBuffer: Arbitrary[ByteBuffer] = Arbitrary(arbitrary[Array[Byte]].map(arr => ByteBuffer.wrap(arr)))

  //Borrowed from here:
  //https://github.com/rallyhealth/scalacheck-ops/blob/master/core/src/main/scala/org/scalacheck/ops/time/ImplicitJavaTimeGenerators.scala
  implicit val arbInstant: Arbitrary[Instant] = {
    import org.scalacheck.Gen._
    Arbitrary {
      for {
        millis <- chooseNum(Instant.MIN.getEpochSecond, Instant.MAX.getEpochSecond)
        nanos <- chooseNum(Instant.MIN.getNano, Instant.MAX.getNano)
      } yield {
        Instant.ofEpochMilli(millis).plusNanos(nanos)
      }
    }
  }

  behavior of "OtherCodecs"

  they should "enable encoding/decoding of BigDecimal" in {
    testFull[BigDecimal]
  }

  they should "enable encoding/decoding of Maps" in {
    testFull[Map[String, String]]
  }

  they should "enable encoding/decoding of Seq" in {
    testFull[Seq[String]]
  }

  they should "enable encoding/decoding of Set" in {
    testFull[Set[String]]
  }

  they should "enable encoding/decoding of List" in {
    testFull[List[String]]
  }

  they should "enable encoding/decoding of ByteString" in {
    testFull[ByteString]
  }

  they should "enable encoding/decoding of ByteBuffer" in {
    testFull[ByteBuffer]
  }

  they should "enable encoding/decoding of UUID" in {
    testFull[UUID]
  }

  they should "enable encoding/decoding of Instant" in {
    testFull[Instant]
  }
}

package io.iohk.cef.network.transport.rlpx

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class ByteUtilsSpec extends FlatSpec with MustMatchers with PropertyChecks {

  import ByteUtils._

  behavior of "ByteUtils"

  it should "convert to and from int" in {
    Seq(1,2,3) foreach { (multiplier: Int) =>
      forAll(Gen.listOfN(multiplier * 4, Gen.choose(Byte.MinValue, Byte.MaxValue))) { (list: List[Byte]) =>
        intsToBytes(bytesToInts(list.toArray)).toList mustBe list
      }
    }
  }
  it should "get int from word" in {
    forAll { (i: Int) =>
      getIntFromWord(intsToBytes(Array(i))) mustBe i
    }
  }
  it should "perform and operations" in {
    forAll { (list1: Array[Byte], list2: Array[Byte]) =>
      and(Array.fill[Byte](list1.size)(0xFF.toByte), list1) mustBe list1
    }
  }
}

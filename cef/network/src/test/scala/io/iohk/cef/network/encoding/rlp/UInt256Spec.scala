package io.iohk.cef.network.encoding.rlp

import akka.util.ByteString
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class UInt256Spec extends FlatSpec with MustMatchers with PropertyChecks {

  val genUInt64: Gen[BigInt] = Gen.chooseNum(Long.MinValue, Long.MaxValue).map(long =>
    BigInt(long) + BigInt(2).pow(63)
  )
  val genUInt256: Gen[BigInt] = Gen.listOfN(4, genUInt64).map(list =>
    list.zip(List(192, 128, 64, 0)).map { case (bigInt, exp)  =>
      bigInt * BigInt(2).pow(exp)
    }.sum
  )

  behavior of "UInt256"
  it should "Have the correct constants" in {
    UInt256.MaxValue mustBe UInt256(BigInt(2).pow(UInt256.Size * 8) - 1)
    UInt256.Zero.isZero mustBe true
    UInt256.Zero mustBe UInt256(0)
    UInt256.One + UInt256.One - UInt256.Two mustBe UInt256.Zero
  }
  it should "be constructed from bytes" in {
    UInt256(Array[Byte](1,0)) mustBe UInt256(256)
  }
  it should "be constructed from other types" in {
    UInt256(true) mustBe UInt256.One
    UInt256(false) mustBe UInt256.Zero
    UInt256(1L) mustBe UInt256.One
    UInt256(1920L) mustBe UInt256(BigInt(1920))
  }
  it should "convert into a ByteString" in {
    forAll(genUInt256) { (s1: BigInt) =>
      UInt256(UInt256(s1).bytes) mustBe UInt256(s1)
    }
  }
  it should "perform binary operations" in {
    forAll(genUInt256, genUInt256) { (s1: BigInt, s2: BigInt) =>
      UInt256(s1) & UInt256(s2) mustBe UInt256(s1 & s2)
      UInt256(s1) | UInt256(s2) mustBe UInt256(s1 | s2)
      UInt256(s1) ^ UInt256(s2) mustBe UInt256(s1 ^ s2)
      - UInt256(s1) mustBe UInt256(-s1)
      ~ UInt256(s1) mustBe UInt256(~s1)
      UInt256(s1) ** UInt256(s2) mustBe UInt256(s1.modPow(s2, BigInt(2).pow(UInt256.Size * 8)))
      UInt256(s1) compare UInt256(s2) mustBe s1.compare(s2)
      UInt256(s1) min UInt256(s2) mustBe UInt256(s1 min s2)
      UInt256(s1) max UInt256(s2) mustBe UInt256(s1 max s2)
    }
  }
  it should "perform multiplications" in {
    forAll(genUInt256, genUInt256) { (s1: BigInt, s2: BigInt) =>
      UInt256(s1) * UInt256(s2) mustBe UInt256(s1 * s2)
    }
    forAll(genUInt256) { (s1: BigInt) =>
      UInt256(s1) * UInt256.Zero mustBe UInt256.Zero
      UInt256.Zero * UInt256(s1) mustBe UInt256.Zero
    }
  }
  it should "perform subtraction" in {
    forAll(genUInt256, genUInt256) { (s1: BigInt, s2: BigInt) =>
      whenever(s2 <= s1) {
        UInt256(s1) - UInt256(s2) mustBe UInt256(s1 - s2)
      }
    }
  }
  it should "perform sum" in {
    forAll(genUInt256, genUInt256) { (s1: BigInt, s2: BigInt) =>
      whenever(s2 + s1 <= UInt256.MaxValue) {
        UInt256(s1) + UInt256(s2) mustBe UInt256(s1 + s2)
      }
    }
  }
  it should "perform division" in {
    forAll(genUInt256, genUInt256) { (s1: BigInt, s2: BigInt) =>
      whenever(s2 > 0) {
        UInt256(s1) / UInt256(s2) mustBe UInt256(s1 / s2)
      }
    }
  }
  it should "perform EVM specific arithmetic when parameter is zero" in {
    forAll(genUInt256, genUInt256) { (s1: BigInt, s2: BigInt) =>
      UInt256(s1) div UInt256.Zero mustBe UInt256.Zero
      UInt256(s1) sdiv UInt256.Zero mustBe UInt256.Zero
      UInt256(s1) mod UInt256.Zero mustBe UInt256.Zero
      UInt256(s1) smod UInt256.Zero mustBe UInt256.Zero
      UInt256(s1) addmod (UInt256(s2), UInt256.Zero) mustBe UInt256.Zero
      UInt256(s1) mulmod (UInt256(s2), UInt256.Zero) mustBe UInt256.Zero
    }
  }
  it should "perform EVM specific arithmetic when parameter is not zero" in {
    forAll(genUInt256, genUInt256, genUInt256) { (s1: BigInt, s2: BigInt, s3: BigInt) =>
      val us1 = UInt256(s1)
      val us2 = UInt256(s2)
      val us3 = UInt256(s3)
      whenever(s2 > 0) {
        us1 div us2 mustBe us1 / us2
        us1 mod us2 mustBe UInt256(s1 mod s2)
        us1 addmod (us2, us3) mustBe UInt256((s1 + s2) mod s3)
        us1 mulmod (us2, us3) mustBe UInt256((s1 * s2) mod s3)
      }
    }
  }
  it should "throw an exception when byte array is larger than size" in {
    intercept[IllegalArgumentException] {
      UInt256(ByteString(Array.fill[Byte](UInt256.Size + 1)(0)))
    }
  }
  it should "convert BigInt into UInt256" in {
    import UInt256._
    forAll(genUInt256) { (s1: BigInt) =>
      s1.toUInt256.toBigInt mustBe s1
    }
  }
  it should "convert from byte" in {
    import UInt256._
    forAll { (b: Byte) =>
      val ui: UInt256 = b
      ui.toByte mustBe b
    }
  }
  it should "convert from Int" in {
    import UInt256._
    forAll(Gen.posNum[Int]) { (n: Int) =>
      val ui: UInt256 = n
      ui.toBigInt mustBe BigInt(n)
    }
  }
  it should "convert from Long" in {
    import UInt256._
    forAll(Gen.posNum[Long]) { (n: Long) =>
      val ui: UInt256 = n
      ui.toBigInt mustBe BigInt(n)
    }
  }
  it should "convert from Boolean" in {
    import UInt256._
    forAll { (n: Boolean) =>
      val ui: UInt256 = n
      val expected = if(n) 1 else 0
      ui.toInt mustBe expected
    }
  }
  it should "calculate bytesize" in {
    UInt256.Zero.byteSize mustBe 0
    UInt256(BigInt(2).pow(7)).byteSize mustBe 1
    UInt256(BigInt(2).pow(8)).byteSize mustBe 2
    UInt256(BigInt(2).pow(255)).byteSize mustBe 32
    UInt256(BigInt(2).pow(248)).byteSize mustBe 32
    UInt256(BigInt(2).pow(247)).byteSize mustBe 31
  }
  it should "convertable to Hex" in {
    forAll(genUInt256) {(n: BigInt) =>
      val ui = UInt256(n)
      BigInt.apply(ui.toHexString.drop(2), 16) mustBe n
    }
  }
  it should "sign extend" in {
    forAll(Gen.posNum[Long]) {(n: Long) =>
      whenever(n >= 0) {
        val ui = UInt256(n)
        ui.signExtend(UInt256(32)) mustBe ui
        ui.signExtend(UInt256(200)) mustBe ui
        (ui.byteSize to 31).foreach(i => {ui.signExtend(i).compare(n) mustBe 0})
      }
    }
  }
}

package io.iohk.cef.network.encoding.nio
import shapeless.{HList, HNil, ::}

trait Default[T] {
  val zero: T
}

object Default {

  implicit val mBoolean: Default[Boolean] = new Default[Boolean] {
    val zero = false
  }
  implicit val mByte: Default[Byte] = new Default[Byte] {
    val zero = 0.toByte
  }
  implicit val mShort: Default[Short] = new Default[Short] {
    val zero = 0
  }
  implicit val mInt: Default[Int] = new Default[Int] {
    val zero = 0
  }
  implicit val mLong: Default[Long] = new Default[Long] {
    val zero = 0
  }
  implicit val mFloat: Default[Float] = new Default[Float] {
    val zero = 0
  }
  implicit val mDouble: Default[Double] = new Default[Double] {
    val zero = 0
  }
  implicit val mChar: Default[Char] = new Default[Char] {
    val zero = 0
  }
  implicit val mString: Default[String] = new Default[String] {
    val zero = ""
  }
  implicit def mHNil: Default[HNil] = new Default[HNil] {
    val zero: HNil = HNil
  }
  implicit def hCons[H, T <: HList](implicit hMon: Default[H], tMon: Default[T]): Default[H :: T] =
    new Default[H :: T] {
      override val zero: H :: T = hMon.zero :: tMon.zero
    }
}

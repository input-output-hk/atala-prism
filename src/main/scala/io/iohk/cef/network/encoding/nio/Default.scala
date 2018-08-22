package io.iohk.cef.network.encoding.nio
import akka.util.ByteString
import shapeless.{::, Generic, HList, HNil, Lazy}

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
  implicit def hCons[H, T <: HList](implicit hMon: Lazy[Default[H]], tMon: Default[T]): Default[H :: T] =
    new Default[H :: T] {
      override val zero: H :: T = hMon.value.zero :: tMon.zero
    }

  implicit val mByteString: Default[ByteString] = new Default[ByteString] {
    override val zero: ByteString = ByteString()
  }

  implicit def genericDefault[T, R](implicit gen: Generic.Aux[T, R], defR: Lazy[Default[R]]): Default[T] =
    new Default[T] {
      override val zero: T = gen.from(defR.value.zero)
    }
}

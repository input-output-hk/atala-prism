package io.iohk.cef.test

import org.scalacheck.{Arbitrary, Gen}
import akka.util.ByteString

trait ScalacheckExctensions {

  private val byteStringGenerator: Gen[ByteString] =
    Arbitrary
      .arbitrary[Array[Byte]]
      .map(ByteString.apply _)

  protected implicit val arbitraryByteStringGenerator: Arbitrary[ByteString] =
    Arbitrary(byteStringGenerator)

  protected def MAX: Int = 30

  def eachTime[T](f: => T): Unit = (1 to MAX).foreach { _ =>
    f
  }

}

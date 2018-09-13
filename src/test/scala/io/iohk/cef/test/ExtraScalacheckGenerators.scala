package io.iohk.cef.test

import org.scalacheck.{Arbitrary, Gen}
import akka.util.ByteString

trait ExtraScalacheckGenerators {

  private val byteStringGenerator: Gen[ByteString] =
    Arbitrary
      .arbitrary[Array[Byte]]
      .map(ByteString.apply _)

  protected implicit val arbitraryByteStringGenerator: Arbitrary[ByteString] =
    Arbitrary(byteStringGenerator)

}

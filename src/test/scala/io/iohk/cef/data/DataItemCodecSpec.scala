package io.iohk.cef.data

import io.iohk.cef.codecs.nio.auto._
import org.scalatest.FlatSpec
import io.iohk.cef.codecs.nio.test.utils.CodecTestingHelpers
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import io.iohk.cef.crypto.test.utils.CryptoEntityArbitraries

class DataItemCodecSpec extends FlatSpec with CodecTestingHelpers with CryptoEntityArbitraries {

  behavior of "CryptoEntities Codecs"

  implicit def arbitraryNonEmptyList[T: Arbitrary]: Arbitrary[NonEmptyList[T]] =
    Arbitrary(arbitrary[List[T]].filter(_.nonEmpty).map{_.asInstanceOf[NonEmptyList[T]]})

  implicit val arbitraryWitness: Arbitrary[Witness] = Arbitrary(Gen.resultOf(Witness.apply _))
  implicit val arbitraryOwner: Arbitrary[Owner] = Arbitrary(Gen.resultOf(Owner.apply _))

  implicit def arbitraryDataItem[T: Arbitrary]: Arbitrary[DataItem[T]] = Arbitrary(Gen.resultOf(DataItem.apply[T] _))

  it should "encode and decode DataItem without problems" in { testFull[DataItem[String]] }
}

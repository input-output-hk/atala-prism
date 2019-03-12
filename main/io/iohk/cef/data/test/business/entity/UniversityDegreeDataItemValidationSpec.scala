package io.iohk.cef.data.business.entity

import java.time.LocalDate

import io.iohk.cef.data._
import io.iohk.cef.utils.NonEmptyList
import io.iohk.codecs.nio.auto._
import io.iohk.crypto._
import org.scalatest.{MustMatchers, WordSpec}

class UniversityDegreeDataItemValidationSpec extends WordSpec with MustMatchers {

  val keyPair = generateSigningKeyPair()
  val newKeyPair = generateSigningKeyPair()

  private implicit val publicKeyStore: Map[String, SigningPublicKey] = Map("UniversityA" -> keyPair.public)

  "A University Degree  Data " should {

    "validate the DataItem" in {
      val data = UniversityDegreeData("UniversityA", "BSC", "Joe Bloc", LocalDate.now())
      val witnessSignature = sign(data, keyPair.`private`)
      val witness = Witness(keyPair.public, witnessSignature)
      val owner = Owner(keyPair.public, sign(LabeledItem.Create(data), keyPair.`private`))
      val dataItem: DataItem[UniversityDegreeData] = DataItem(data, List(witness), NonEmptyList(owner))
      UniversityDegreeData.universityDegreeValidation.validate(dataItem) mustBe Right(())
    }

    "fail with InvalidUniversitySignatureError for  invalid Signature provided" in {
      val data = UniversityDegreeData("UniversityA", "BSC", "Joe Bloc", LocalDate.now())
      val witnessSignature = sign(data, newKeyPair.`private`)
      val witness = Witness(keyPair.public, witnessSignature)
      val owner = Owner(keyPair.public, sign(LabeledItem.Create(data), keyPair.`private`))
      val dataItem: DataItem[UniversityDegreeData] = DataItem(data, List(witness), NonEmptyList(owner))
      UniversityDegreeData.universityDegreeValidation.validate(dataItem) mustBe Left(
        InvalidUniversitySignatureError("UniversityA")
      )
    }

    "fail with NoWitnessProvided" in {
      val data = UniversityDegreeData("UniversityA", "BSC", "Joe Bloc", LocalDate.now())
      val owner = Owner(keyPair.public, sign(LabeledItem.Create(data), keyPair.`private`))
      val dataItem: DataItem[UniversityDegreeData] =
        DataItem(data, List.empty[Witness], NonEmptyList(owner))
      UniversityDegreeData.universityDegreeValidation.validate(dataItem) mustBe Left(
        NoWitnessProvided("UniversityA")
      )
    }

    "fail with UniversityPublicKeyIsUnknown" in {
      val data = UniversityDegreeData("UniversityA", "BSC", "Joe Bloc", LocalDate.now())
      val witnessSignature = sign(data, newKeyPair.`private`)
      val witness = Witness(keyPair.public, witnessSignature)
      val owner = Owner(keyPair.public, sign(LabeledItem.Create(data), keyPair.`private`))
      implicit val publicKeyStore: Map[String, SigningPublicKey] = Map("UniversityB" -> keyPair.public)

      val dataItem: DataItem[UniversityDegreeData] = DataItem(data, List(witness), NonEmptyList(owner))
      UniversityDegreeData.universityDegreeValidation.validate(dataItem) mustBe Left(
        UniversityPublicKeyIsUnknown("UniversityA", witness)
      )
    }
  }
}

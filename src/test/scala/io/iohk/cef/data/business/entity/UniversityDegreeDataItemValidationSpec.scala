package io.iohk.cef.data.business.entity

import java.time.LocalDate

import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data._
import org.scalactic.Every
import org.scalatest.{MustMatchers, WordSpec}

class UniversityDegreeDataItemValidationSpec extends WordSpec with MustMatchers {

  val keyPair = generateSigningKeyPair()
  val newKeyPair = generateSigningKeyPair()
  private val defaultOwner = Owner(generateSigningKeyPair().public)

  private implicit val publicKeyStore: Map[String, SigningPublicKey] = Map("UniversityA" -> keyPair.public)

  "A University Degree  Data " should {

    "validate the DataItem" in {
      val data = UniversityDegreeData("UniversityA", "BSC", "Joe Bloc", LocalDate.now())
      val witnessSignature = sign(data, keyPair.`private`)
      val witness = Witness(keyPair.public, witnessSignature)
      val owner = Owner(keyPair.public)
      val dataItem: DataItem[UniversityDegreeData] = DataItem("universityId", data, List(witness), Every(owner))
      UniversityDegreeData.universityDegreeValidation.validate(dataItem) mustBe Right(())
    }

    "fail with InvalidUniversitySignatureError for  invalid Signature provided" in {
      val data = UniversityDegreeData("UniversityA", "BSC", "Joe Bloc", LocalDate.now())
      val witnessSignature = sign(data, newKeyPair.`private`)
      val witness = Witness(keyPair.public, witnessSignature)
      val owner = Owner(keyPair.public)
      val dataItem: DataItem[UniversityDegreeData] = DataItem("universityId", data, List(witness), Every(owner))
      UniversityDegreeData.universityDegreeValidation.validate(dataItem) mustBe Left(
        InvalidUniversitySignatureError("UniversityA", "universityId"))
    }

    "fail with NoWitnessProvided" in {
      val data = UniversityDegreeData("UniversityA", "BSC", "Joe Bloc", LocalDate.now())
      val witnessSignature = sign(data, keyPair.`private`)
      //val witness = Witness(keyPair.public, witnessSignature)
      val owner = Owner(keyPair.public)
      val dataItem: DataItem[UniversityDegreeData] = DataItem("universityId", data, List.empty[Witness], Every(owner))
      UniversityDegreeData.universityDegreeValidation.validate(dataItem) mustBe Left(
        NoWitnessProvided("UniversityA", "universityId"))
    }

    "fail with UniversityPublicKeyIsUnknown" in {
      val data = UniversityDegreeData("UniversityA", "BSC", "Joe Bloc", LocalDate.now())
      val witnessSignature = sign(data, newKeyPair.`private`)
      val witness = Witness(keyPair.public, witnessSignature)
      val owner = Owner(keyPair.public)
      implicit val publicKeyStore: Map[String, SigningPublicKey] = Map("UniversityB" -> keyPair.public)

      val dataItem: DataItem[UniversityDegreeData] = DataItem("universityId", data, List(witness), Every(owner))
      UniversityDegreeData.universityDegreeValidation.validate(dataItem) mustBe Left(
        UniversityPublicKeyIsUnknown("UniversityA", witness, "universityId"))
    }
  }
}

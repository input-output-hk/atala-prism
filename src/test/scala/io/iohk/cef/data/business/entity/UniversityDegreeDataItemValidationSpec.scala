package io.iohk.cef.data.business.entity

import java.time.LocalDate

import io.iohk.cef.crypto._
import io.iohk.cef.data._
import org.scalatest.{MustMatchers, WordSpec}
import io.iohk.cef.codecs.nio._
class UniversityDegreeDataItemValidationSpec extends WordSpec with MustMatchers {

  val keyPair = generateSigningKeyPair()

  private implicit val publicKeyStore: Map[String, SigningPublicKey] = Map("UniversityA" -> keyPair.public)
  //private implicit val enc = NioEncoder[UniversityDegreeData]

  "A University Degree  Data " should {
    "validate the DataItem" in {
      val data = UniversityDegreeData("UniversityA", "BSC", "Joe Bloc", LocalDate.now())
      val witnessSignature = sign(data, keyPair.`private`)
      val witness = Witness(keyPair.public, witnessSignature)
      val owner = Owner(keyPair.public)
      val dataItem: DataItem[UniversityDegreeData] = DataItem("universityId", data, List(witness), List(owner))
      UniversityDegreeData.universityDegreeValidation.validate(dataItem) mustBe Right(())
    }
  }
}

package io.iohk.atala.prism.mirror.trisa

import com.google.protobuf.any.Any
import io.iohk.atala.mirror.protos.ivms101.{Beneficiary, IdentityPayload, Person}
import io.iohk.atala.mirror.protos.trisa.TransactionData
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.EitherValues
import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import io.iohk.atala.prism.util.BytesOps

class TrisaAesGcmSpec extends AnyWordSpec with Matchers with EitherValues with ScalaCheckPropertyChecks {

  "TrisaAesGcm" should {
    "decrypt correct data " in new Fixtures {
      forAll(dataGen) { data =>
        TrisaAesGcm.encrypt(data).flatMap(TrisaAesGcm.decrypt).map(_.toSeq) mustBe Right(data.toSeq)
      }
      TrisaAesGcm.decrypt(trisaAesGcmEncrypted).map(_.toSeq) mustBe Right(
        BytesOps
          .hexToBytes(
            "019b96ba010c00015200013801198001cb7fa70000260180ffffff007200007ff5ff6af661aa00ff0c7f7f86019f767f01"
          )
          .toSeq
      )
    }

    "encrypt and decrypt transaction data " in new Fixtures {
      TrisaAesGcm
        .encryptTransactionData(transactionData)
        .flatMap(TrisaAesGcm.decrypt)
        .map(TransactionData.parseFrom) mustBe Right(transactionData)
    }

    "fail with incorrect cipher secret" in new Fixtures {
      val invalidData =
        trisaAesGcmEncrypted.copy(cipherSecret = TrisaAesGcm.randomBytes(TrisaAesGcm.KEY_SIZE).toIndexedSeq)

      TrisaAesGcm
        .decrypt(invalidData)
        .left
        .value mustBe a[TrisaAesGcm.TrisaAesGcmException]
    }

    "fail with incorrect hmac signature" in new Fixtures {
      val invalidData =
        trisaAesGcmEncrypted.copy(hmac = TrisaAesGcm.randomBytes(32).toIndexedSeq)

      TrisaAesGcm
        .decrypt(invalidData)
        .left
        .value mustBe a[TrisaAesGcm.TrisaAesGcmException]
    }

    "compute HMAC SHA256" in {
      BytesOps.bytesToHex(
        TrisaAesGcm.hmacSha256("test".getBytes, "secret".getBytes)
      ) mustBe "0329a06b62cd16b33eb6792be8c60b158d89a2ee3a876fce9a881ebb488c0914"
    }
  }

  trait Fixtures {
    val dataGen: Gen[Array[Byte]] =
      Gen.nonEmptyListOf(arbitrary[Byte]).map(_.toArray)

    val trisaAesGcmEncrypted = TrisaAesGcm.TrisaAesGcmEncryptedData(
      data = BytesOps
        .hexToBytes(
          "762ead1c035a8043016320ddf75ba1360942d590594dfe30de4b37fd1b8c2ebb971f08295f658251b3cdb01da1baa88e948d49e2a8a1c34ed0ebb54676dc1d8ecd17aa9c328ff0326371dbb4f9"
        )
        .toIndexedSeq,
      cipherSecret =
        BytesOps.hexToBytes("22d65f2cf69b3e2120bce2f8ce26bab3e38de654f877f58cb2f638e472a59181").toIndexedSeq,
      hmac = BytesOps.hexToBytes("b72d0bdd15258753faefb13fe6200de3125a60ac9b50a3f4f8c6977c93488dd8").toIndexedSeq,
      hmacSecret = BytesOps.hexToBytes("22d65f2cf69b3e2120bce2f8ce26bab3e38de654f877f58cb2f638e472a59181").toIndexedSeq
    )

    val transactionData = TransactionData(
      identity = Some(
        Any(
          value = IdentityPayload(beneficiary = Some(Beneficiary(beneficiaryPersons = Seq(Person())))).toByteString,
          typeUrl = "type.googleapis.com/ivms101.IdentityPayload"
        )
      )
    )
  }

}

package io.iohk.node.repositories

import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.errors.NodeError.UnknownValueError
import io.iohk.node.models.DIDData
import io.iohk.node.operations.TimestampInfo
import io.iohk.node.repositories.daos.CredentialsDAO.CreateCredentialData
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationLong

class CredentialsRepositorySpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  override val tables = List("credentials", "public_keys", "did_data")

  val didOperationDigest = digestGen(0, 1)
  val didSuffix = didSuffixFromDigest(didOperationDigest)
  val didData = DIDData(didSuffix, Nil, didOperationDigest)

  val credentialOperationDigest = digestGen(1, 2)
  val credentialId = credentialIdFromDigest(credentialOperationDigest)
  val credentialDigest = digestGen(127, 1)
  val issuanceDate = TimestampInfo.dummyTime
  val revocationDate = TimestampInfo.dummyTime
  val createCredentialData =
    CreateCredentialData(credentialId, credentialOperationDigest, didSuffix, credentialDigest, issuanceDate)

  "CredentialsRepository" should {
    "retrieve inserted credential" in {
      val result = (for {
        _ <- didDataRepository.create(didData, issuanceDate)
        _ <- credentialsRepository.create(
          createCredentialData
        )
        credential <- credentialsRepository.find(credentialId)
      } yield credential).value.futureValue.right.value

      result.credentialId mustBe credentialId
      result.issuerDIDSuffix mustBe didSuffix
      result.contentHash mustBe credentialDigest
      result.revokedOn mustBe None
    }

    "return error if retrieved credential not found" in {
      val otherCredentialId = credentialIdFromDigest(digestGen(1, 3))

      val result = (for {
        _ <- didDataRepository.create(didData, issuanceDate)
        _ <- credentialsRepository.create(createCredentialData)
        credential <- credentialsRepository.find(otherCredentialId)
      } yield credential).value.futureValue.left.value

      result mustBe an[UnknownValueError]
    }

    "revoke credential" in {
      val (revocation, credential) = (for {
        _ <- didDataRepository.create(didData, issuanceDate)
        _ <- credentialsRepository.create(createCredentialData)
        revocation <- credentialsRepository.revoke(credentialId, revocationDate)
        credential <- credentialsRepository.find(credentialId)
      } yield (revocation, credential)).value.futureValue.right.value

      revocation mustBe true
      credential.revokedOn mustBe Some(revocationDate)
    }

    "return false if revoked credential not found" in {
      val otherCredentialId = credentialIdFromDigest(digestGen(1, 3))

      val result = (for {
        _ <- didDataRepository.create(didData, TimestampInfo.dummyTime)
        _ <- credentialsRepository.create(createCredentialData)
        revocation <- credentialsRepository.revoke(otherCredentialId, revocationDate)
      } yield revocation).value.futureValue.right.value

      result mustBe false
    }
  }
}

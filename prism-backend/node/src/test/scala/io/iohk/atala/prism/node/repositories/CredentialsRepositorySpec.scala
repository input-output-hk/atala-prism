package io.iohk.atala.prism.node.repositories

import java.time.Instant

import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.DIDData
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.repositories.daos.CredentialsDAO.CreateCredentialData
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationLong

class CredentialsRepositorySpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  val didOperationDigest = digestGen(0, 1)
  val didSuffix = didSuffixFromDigest(didOperationDigest)
  val didData = DIDData(didSuffix, Nil, didOperationDigest)

  private val dummyTimestampInfo = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
  private val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    dummyTimestampInfo
  )
  val credentialOperationDigest = digestGen(1, 2)
  val credentialId = credentialIdFromDigest(credentialOperationDigest)
  val credentialDigest = digestGen(127, 1)
  val issuanceDate = dummyTimestampInfo
  val issuanceLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    issuanceDate
  )
  val revocationDate = dummyTimestampInfo
  val revocationLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    revocationDate
  )
  val createCredentialData =
    CreateCredentialData(credentialId, credentialOperationDigest, didSuffix, credentialDigest, issuanceLedgerData)

  "CredentialsRepository" should {
    "retrieve inserted credential" in {
      val result = (for {
        _ <- didDataRepository.create(didData, issuanceLedgerData)
        _ <- credentialsRepository.create(
          createCredentialData
        )
        credential <- credentialsRepository.find(credentialId)
      } yield credential).value.futureValue.toOption.value

      result.credentialId mustBe credentialId
      result.issuerDIDSuffix mustBe didSuffix
      result.contentHash mustBe credentialDigest
      result.revokedOn mustBe None
    }

    "return error if retrieved credential not found" in {
      val otherCredentialId = credentialIdFromDigest(digestGen(1, 3))

      val result = (for {
        _ <- didDataRepository.create(didData, issuanceLedgerData)
        _ <- credentialsRepository.create(createCredentialData)
        credential <- credentialsRepository.find(otherCredentialId)
      } yield credential).value.futureValue.left.value

      result mustBe an[UnknownValueError]
    }

    "revoke credential" in {
      val (revocation, credential) = (for {
        _ <- didDataRepository.create(didData, issuanceLedgerData)
        _ <- credentialsRepository.create(createCredentialData)
        revocation <- credentialsRepository.revoke(credentialId, revocationLedgerData)
        credential <- credentialsRepository.find(credentialId)
      } yield (revocation, credential)).value.futureValue.toOption.value

      revocation mustBe true
      credential.revokedOn mustBe Some(revocationLedgerData.timestampInfo)
    }

    "return false if revoked credential not found" in {
      val otherCredentialId = credentialIdFromDigest(digestGen(1, 3))

      val result = (for {
        _ <- didDataRepository.create(didData, dummyLedgerData)
        _ <- credentialsRepository.create(createCredentialData)
        revocation <- credentialsRepository.revoke(otherCredentialId, revocationLedgerData)
      } yield revocation).value.futureValue.toOption.value

      result mustBe false
    }
  }
}

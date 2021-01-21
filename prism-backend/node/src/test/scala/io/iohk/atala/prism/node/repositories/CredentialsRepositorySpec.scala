package io.iohk.atala.prism.node.repositories

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.DIDData
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.repositories.daos.CredentialsDAO.CreateCredentialData
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import java.time.Instant

import io.iohk.atala.prism.node.DataPreparation

class CredentialsRepositorySpec extends AtalaWithPostgresSpec {
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  private val didOperationDigest = digestGen(0, 1)
  private val didSuffix = didSuffixFromDigest(didOperationDigest)
  private val didData = DIDData(didSuffix, Nil, didOperationDigest)

  private val dummyTimestampInfo = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)

  private val credentialOperationDigest = digestGen(1, 2)
  private val credentialId = credentialIdFromDigest(credentialOperationDigest)
  private val credentialDigest = digestGen(127, 1)
  private val issuanceDate = dummyTimestampInfo
  private val issuanceLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    issuanceDate
  )
  private val createCredentialData =
    CreateCredentialData(credentialId, credentialOperationDigest, didSuffix, credentialDigest, issuanceLedgerData)

  implicit val implicitXa = database

  "CredentialsRepository.getCredentialState" should {
    "retrieve inserted credential" in {
      DataPreparation.createDID(didData, issuanceLedgerData)
      DataPreparation.createCredential(createCredentialData)

      val result = credentialsRepository
        .getCredentialState(credentialId)
        .value
        .futureValue
        .toOption
        .value

      result.credentialId mustBe credentialId
      result.issuerDIDSuffix mustBe didSuffix
      result.contentHash mustBe credentialDigest
      result.revokedOn mustBe None
    }

    "return error if retrieved credential not found" in {
      val otherCredentialId = credentialIdFromDigest(digestGen(1, 3))

      DataPreparation.createDID(didData, issuanceLedgerData)
      DataPreparation.createCredential(createCredentialData)

      val result = credentialsRepository
        .getCredentialState(otherCredentialId)
        .value
        .futureValue
        .left
        .value

      result mustBe an[UnknownValueError]
    }

    "getCredentialTransactionInfo returns transaction info when the credential is published" in {
      DataPreparation.createDID(didData, issuanceLedgerData)
      DataPreparation.createCredential(createCredentialData)
      val transactionInfo = credentialsRepository
        .getCredentialTransactionInfo(credentialId)
        .value
        .futureValue
        .toOption
        .value

      transactionInfo.value.transactionId must be(createCredentialData.ledgerData.transactionId)
      transactionInfo.value.ledger must be(createCredentialData.ledgerData.ledger)
      transactionInfo.value.block must be(empty)
    }

    "getCredentialTransactionInfo returns Right(None) when the credential is not published" in {
      val transactionInfo = credentialsRepository
        .getCredentialTransactionInfo(credentialId)
        .value
        .futureValue
        .toOption
        .value

      transactionInfo must be(empty)
    }
  }
}

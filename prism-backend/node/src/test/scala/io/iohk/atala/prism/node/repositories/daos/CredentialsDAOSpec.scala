package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.models.DIDData
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.repositories.daos.CredentialsDAO.CreateCredentialData
import io.iohk.atala.prism.node.repositories.{credentialIdFromDigest, didSuffixFromDigest, digestGen}

import org.scalatest.OptionValues._

class CredentialsDAOSpec extends AtalaWithPostgresSpec {

  private val didOperationDigest = digestGen(0, 1)
  private val didSuffix = didSuffixFromDigest(didOperationDigest)
  private val didData = DIDData(didSuffix, Nil, didOperationDigest)

  private val dummyTimestampInfo = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
  private val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    dummyTimestampInfo
  )

  private val credentialOperationDigest = digestGen(1, 2)
  private val credentialId = credentialIdFromDigest(credentialOperationDigest)
  private val credentialDigest = digestGen(127, 1)
  private val issuanceDate = dummyTimestampInfo
  private val issuanceLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    issuanceDate
  )
  private val revocationDate = dummyTimestampInfo
  private val revocationLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    revocationDate
  )
  private val createCredentialData =
    CreateCredentialData(credentialId, credentialOperationDigest, didSuffix, credentialDigest, issuanceLedgerData)

  "CredentialsDAO" should {
    "revoke credential" in {
      DataPreparation.createDID(didData, issuanceLedgerData)
      DataPreparation.createCredential(createCredentialData)
      val wasRevoked = DataPreparation.revokeCredential(credentialId, revocationLedgerData)

      wasRevoked mustBe true
    }

    "return false if revoked credential not found" in {
      val otherCredentialId = credentialIdFromDigest(digestGen(1, 3))

      DataPreparation.createDID(didData, dummyLedgerData)
      DataPreparation.createCredential(createCredentialData)
      val wasRevoked = DataPreparation.revokeCredential(otherCredentialId, revocationLedgerData)

      wasRevoked mustBe false
    }
  }
}

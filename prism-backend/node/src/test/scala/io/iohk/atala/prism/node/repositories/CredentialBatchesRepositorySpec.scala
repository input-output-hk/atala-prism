package io.iohk.atala.prism.node.repositories

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.kotlin.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DIDSuffix
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, LedgerData}
import org.scalatest.OptionValues._

import java.time.Instant
import cats.effect.IO
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.models.DIDData

import io.iohk.atala.prism.interop.toScalaSDK._

class CredentialBatchesRepositorySpec extends AtalaWithPostgresSpec {

  import CredentialBatchesRepositorySpec._

  private lazy implicit val repository: CredentialBatchesRepository[IO] = CredentialBatchesRepository(database)

  private val dummyTimestampInfo = new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
  private val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    dummyTimestampInfo
  )

  "CredentialsRepository.getCredentialRevocationTime" should {
    "return empty timestamp when there is no data associated to the credential and batch" in {
      val randomBatchId = CredentialBatchId.random()
      val randomCredentialHash = SHA256Digest.compute("random".getBytes())

      revocationTime(randomBatchId, randomCredentialHash) must be(None)
    }

    "return proper timestamp when there is data associated to the credential and batch" in {
      val randomBatchId = CredentialBatchId.random()
      val randomCredentialHash1 = SHA256Digest.compute("random".getBytes())
      val randomCredentialHash2 = SHA256Digest.compute("another random".getBytes())
      val randomRevocationTime = new TimestampInfo(Instant.now().toEpochMilli, 10, 100)
      val randomRevocationLedgerData = LedgerData(
        TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
        Ledger.InMemory,
        randomRevocationTime
      )

      val randomIssuerDIDSuffix = DIDSuffix.fromDigest(SHA256Digest.compute("did".getBytes()))
      val randomLastOperation = SHA256Digest.compute("lastOperation".getBytes())
      val randomMerkleRoot = new MerkleRoot(SHA256Digest.compute("merkleRoot".getBytes()))
      val randomIssuedOnTime = LedgerData(
        TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
        Ledger.InMemory,
        dummyTimestampInfo
      )

      registerDID(randomIssuerDIDSuffix)

      DataPreparation.createBatch(
        randomBatchId,
        randomLastOperation,
        randomIssuerDIDSuffix,
        randomMerkleRoot,
        randomIssuedOnTime
      )

      DataPreparation.revokeCredentials(
        randomBatchId,
        List(randomCredentialHash1, randomCredentialHash2),
        randomRevocationLedgerData
      )

      revocationTime(randomBatchId, randomCredentialHash1) must be(
        Some(randomRevocationLedgerData)
      )
      revocationTime(randomBatchId, randomCredentialHash2) must be(
        Some(randomRevocationLedgerData)
      )
    }
  }

  "CredentialsRepository.getBatchState" should {
    "return empty when the batch is unknown" in {
      val randomBatchId = CredentialBatchId.random()

      val response = repository
        .getBatchState(randomBatchId)
        .unsafeRunSync()
        .toOption
        .flatten

      response must be(empty)
    }

    "return proper data when there is non-revoked batch data" in {
      val randomBatchId = CredentialBatchId.random()
      val randomIssuerDIDSuffix = DIDSuffix.fromDigest(SHA256Digest.compute("did".getBytes()))
      val randomLastOperation = SHA256Digest.compute("lastOperation".getBytes())
      val randomMerkleRoot = new MerkleRoot(SHA256Digest.compute("merkleRoot".getBytes()))
      val randomIssuedOnLedgerData = LedgerData(
        TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
        Ledger.InMemory,
        dummyTimestampInfo
      )

      registerDID(randomIssuerDIDSuffix)

      DataPreparation.createBatch(
        randomBatchId,
        randomLastOperation,
        randomIssuerDIDSuffix,
        randomMerkleRoot,
        randomIssuedOnLedgerData
      )

      val expectedState = CredentialBatchState(
        batchId = randomBatchId,
        issuerDIDSuffix = randomIssuerDIDSuffix,
        merkleRoot = randomMerkleRoot,
        issuedOn = randomIssuedOnLedgerData,
        revokedOn = None,
        lastOperation = randomLastOperation
      )

      repository
        .getBatchState(randomBatchId)
        .unsafeRunSync()
        .toOption
        .flatten must be(Some(expectedState))
    }

    "return proper data when the batch was revoked" in {
      val randomBatchId = CredentialBatchId.random()
      val randomIssuerDIDSuffix = DIDSuffix.fromDigest(SHA256Digest.compute("did".getBytes()))
      val randomLastOperation = SHA256Digest.compute("lastOperation".getBytes())
      val randomMerkleRoot = new MerkleRoot(SHA256Digest.compute("merkleRoot".getBytes()))
      val randomIssuedOnLedgerData = dummyLedgerData

      val randomRevocationTime = new TimestampInfo(Instant.now().toEpochMilli, 10, 100)
      val randomRevocationLedgerData = LedgerData(
        TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
        Ledger.InMemory,
        randomRevocationTime
      )

      registerDID(randomIssuerDIDSuffix)

      DataPreparation.createBatch(
        randomBatchId,
        randomLastOperation,
        randomIssuerDIDSuffix,
        randomMerkleRoot,
        randomIssuedOnLedgerData
      )

      DataPreparation.revokeCredentialBatch(randomBatchId, randomRevocationLedgerData)

      val expectedState = CredentialBatchState(
        batchId = randomBatchId,
        issuerDIDSuffix = randomIssuerDIDSuffix,
        merkleRoot = randomMerkleRoot,
        issuedOn = randomIssuedOnLedgerData,
        revokedOn = Some(randomRevocationLedgerData),
        lastOperation = randomLastOperation
      )

      repository
        .getBatchState(randomBatchId)
        .unsafeRunSync()
        .toOption
        .flatten must be(Some(expectedState))
    }
  }
}

object CredentialBatchesRepositorySpec {
  private def registerDID(didSuffix: DIDSuffix)(implicit database: Transactor[IO]): Unit = {
    val lastOperation = SHA256Digest.compute("a random did create operation".getBytes())
    val dummyTimestampInfo = new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
    val dummyLedgerData = LedgerData(
      TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
      Ledger.InMemory,
      dummyTimestampInfo
    )
    DataPreparation.createDID(
      DIDData(didSuffix, keys = Nil, lastOperation),
      dummyLedgerData
    )
  }

  private def revocationTime(batchId: CredentialBatchId, credentialHash: SHA256Digest)(implicit
      repository: CredentialBatchesRepository[IO]
  ): Option[LedgerData] = {
    repository
      .getCredentialRevocationTime(batchId, credentialHash)
      .unsafeRunSync()
      .toOption
      .flatten
  }
}

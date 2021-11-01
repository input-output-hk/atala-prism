package io.iohk.atala.prism.node.repositories

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{MerkleRoot, Sha256, Sha256Digest}
import io.iohk.atala.prism.models.{DidSuffix, Ledger, TransactionId}
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, LedgerData}
import org.scalatest.OptionValues._

import java.time.Instant
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.models.DIDData
import tofu.logging.Logging

class CredentialBatchesRepositorySpec extends AtalaWithPostgresSpec {

  import CredentialBatchesRepositorySpec._

  private val logs = Logging.Make.plain[IO]

  private lazy implicit val repository: CredentialBatchesRepository[IO] =
    CredentialBatchesRepository.unsafe(database, logs)

  private val dummyTimestampInfo =
    new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
  private val dummyLedgerData = LedgerData(
    TransactionId
      .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
      .value,
    Ledger.InMemory,
    dummyTimestampInfo
  )

  "CredentialsRepository.getCredentialRevocationTime" should {
    "return empty timestamp when there is no data associated to the credential and batch" in {
      val randomBatchId = CredentialBatchId.random()
      val randomCredentialHash = Sha256.compute("random".getBytes())

      revocationTime(randomBatchId, randomCredentialHash) must be(None)
    }

    "return proper timestamp when there is data associated to the credential and batch" in {
      val randomBatchId = CredentialBatchId.random()
      val randomCredentialHash1 = Sha256.compute("random".getBytes())
      val randomCredentialHash2 = Sha256.compute("another random".getBytes())
      val randomRevocationTime =
        new TimestampInfo(Instant.now().toEpochMilli, 10, 100)
      val randomRevocationLedgerData = LedgerData(
        TransactionId
          .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
          .value,
        Ledger.InMemory,
        randomRevocationTime
      )

      val randomIssuerDIDSuffix =
        DidSuffix(Sha256.compute("did".getBytes()).getHexValue)
      val randomLastOperation = Sha256.compute("lastOperation".getBytes())
      val randomMerkleRoot =
        new MerkleRoot(Sha256.compute("merkleRoot".getBytes()))
      val randomIssuedOnTime = LedgerData(
        TransactionId
          .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
          .value,
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
      val randomIssuerDIDSuffix =
        DidSuffix(Sha256.compute("did".getBytes()).getHexValue)
      val randomLastOperation = Sha256.compute("lastOperation".getBytes())
      val randomMerkleRoot =
        new MerkleRoot(Sha256.compute("merkleRoot".getBytes()))
      val randomIssuedOnLedgerData = LedgerData(
        TransactionId
          .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
          .value,
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
      val randomIssuerDIDSuffix =
        DidSuffix(Sha256.compute("did".getBytes()).getHexValue)
      val randomLastOperation = Sha256.compute("lastOperation".getBytes())
      val randomMerkleRoot =
        new MerkleRoot(Sha256.compute("merkleRoot".getBytes()))
      val randomIssuedOnLedgerData = dummyLedgerData

      val randomRevocationTime =
        new TimestampInfo(Instant.now().toEpochMilli, 10, 100)
      val randomRevocationLedgerData = LedgerData(
        TransactionId
          .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
          .value,
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

      DataPreparation.revokeCredentialBatch(
        randomBatchId,
        randomRevocationLedgerData
      )

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
  private def registerDID(
      didSuffix: DidSuffix
  )(implicit database: Transactor[IO]): Unit = {
    val lastOperation =
      Sha256.compute("a random did create operation".getBytes())
    val dummyTimestampInfo =
      new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
    val dummyLedgerData = LedgerData(
      TransactionId
        .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
        .value,
      Ledger.InMemory,
      dummyTimestampInfo
    )
    DataPreparation.createDID(
      DIDData(didSuffix, keys = Nil, lastOperation),
      dummyLedgerData
    )
  }

  private def revocationTime(
      batchId: CredentialBatchId,
      credentialHash: Sha256Digest
  )(implicit
      repository: CredentialBatchesRepository[IO]
  ): Option[LedgerData] = {
    repository
      .getCredentialRevocationTime(batchId, credentialHash)
      .unsafeRunSync()
      .toOption
      .flatten
  }
}

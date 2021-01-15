package io.iohk.atala.prism.node.repositories

import java.time.Instant

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, LedgerData}
import io.iohk.atala.prism.node.repositories.daos.{CredentialBatchesDAO, DIDDataDAO}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO.CreateCredentialBatchData
import io.iohk.atala.prism.AtalaWithPostgresSpec
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationLong

class CredentialBatchesRepositorySpec extends AtalaWithPostgresSpec {

  import CredentialBatchesRepositorySpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)

  private lazy implicit val repository = new CredentialBatchesRepository(database)

  private val dummyTimestampInfo = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
  private val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    dummyTimestampInfo
  )

  "CredentialsRepository.getCredentialRevocationTime" should {
    "return empty timestamp when there is no data associated to the credential and batch" in {
      val randomBatchId = CredentialBatchId.random()
      val randomCredentialHash = SHA256Digest.compute("random".getBytes())

      revocationTime(randomBatchId, randomCredentialHash) must be(empty)
    }

    "return proper timestamp when there is data associated to the credential and batch" in {
      val randomBatchId = CredentialBatchId.random()
      val randomCredentialHash1 = SHA256Digest.compute("random".getBytes())
      val randomCredentialHash2 = SHA256Digest.compute("another random".getBytes())
      val randomRevocationTime = TimestampInfo(Instant.now(), 10, 100)
      val randomRevocationLedgerData = LedgerData(
        TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
        Ledger.InMemory,
        randomRevocationTime
      )

      val randomIssuerDIDSuffix = DIDSuffix.unsafeFromDigest(SHA256Digest.compute("did".getBytes()))
      val randomLastOperation = SHA256Digest.compute("lastOperation".getBytes())
      val randomMerkleRoot = MerkleRoot(SHA256Digest.compute("merkleRoot".getBytes()))
      val randomIssuedOnTime = LedgerData(
        TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
        Ledger.InMemory,
        dummyTimestampInfo
      )

      registerDID(randomIssuerDIDSuffix)

      createBatch(
        randomBatchId,
        randomLastOperation,
        randomIssuerDIDSuffix,
        randomMerkleRoot,
        randomIssuedOnTime
      )

      revokeCredentials(
        randomBatchId,
        List(randomCredentialHash1, randomCredentialHash2),
        randomRevocationLedgerData
      )

      revocationTime(randomBatchId, randomCredentialHash1) must be(Some(randomRevocationLedgerData))
      revocationTime(randomBatchId, randomCredentialHash2) must be(Some(randomRevocationLedgerData))
    }
  }

  "CredentialsRepository.getBatchState" should {
    "fail when the batch is unknown" in {
      val randomBatchId = CredentialBatchId.random()

      val err = repository
        .getBatchState(randomBatchId)
        .value
        .futureValue
        .left
        .toOption
        .value

      err must be(UnknownValueError("batchId", randomBatchId.id))
    }

    "return proper data when there is non-revoked batch data" in {
      val randomBatchId = CredentialBatchId.random()
      val randomIssuerDIDSuffix = DIDSuffix.unsafeFromDigest(SHA256Digest.compute("did".getBytes()))
      val randomLastOperation = SHA256Digest.compute("lastOperation".getBytes())
      val randomMerkleRoot = MerkleRoot(SHA256Digest.compute("merkleRoot".getBytes()))
      val randomIssuedOnLedgerData = LedgerData(
        TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
        Ledger.InMemory,
        dummyTimestampInfo
      )

      registerDID(randomIssuerDIDSuffix)

      createBatch(
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
        .value
        .futureValue
        .toOption
        .value must be(expectedState)
    }

    "return proper data when the batch was revoked" in {
      val randomBatchId = CredentialBatchId.random()
      val randomIssuerDIDSuffix = DIDSuffix.unsafeFromDigest(SHA256Digest.compute("did".getBytes()))
      val randomLastOperation = SHA256Digest.compute("lastOperation".getBytes())
      val randomMerkleRoot = MerkleRoot(SHA256Digest.compute("merkleRoot".getBytes()))
      val randomIssuedOnLedgerData = dummyLedgerData

      val randomRevocationTime = TimestampInfo(Instant.now(), 10, 100)
      val randomRevocationLedgerData = LedgerData(
        TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
        Ledger.InMemory,
        randomRevocationTime
      )

      registerDID(randomIssuerDIDSuffix)

      createBatch(
        randomBatchId,
        randomLastOperation,
        randomIssuerDIDSuffix,
        randomMerkleRoot,
        randomIssuedOnLedgerData
      )

      revokeCredentialBatch(randomBatchId, randomRevocationLedgerData)

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
        .value
        .futureValue
        .toOption
        .value must be(expectedState)
    }
  }
}

object CredentialBatchesRepositorySpec {
  private def registerDID(didSuffix: DIDSuffix)(implicit database: Transactor[IO]): Unit = {
    val lastOperation = SHA256Digest.compute("a random did create operation".getBytes())
    val dummyTimestampInfo = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
    val dummyLedgerData = LedgerData(
      TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
      Ledger.InMemory,
      dummyTimestampInfo
    )
    DIDDataDAO
      .insert(didSuffix, lastOperation, dummyLedgerData)
      .transact(database)
      .unsafeRunSync()
  }

  private def createBatch(
      batchId: CredentialBatchId,
      lastOperation: SHA256Digest,
      issuerDIDSuffix: DIDSuffix,
      merkleRoot: MerkleRoot,
      issuedOn: LedgerData
  )(implicit database: Transactor[IO]): Unit = {
    CredentialBatchesDAO
      .insert(
        CreateCredentialBatchData(
          batchId = batchId,
          lastOperation = lastOperation,
          issuerDIDSuffix = issuerDIDSuffix,
          merkleRoot = merkleRoot,
          ledgerData = issuedOn
        )
      )
      .transact(database)
      .unsafeRunSync()
  }

  private def revokeCredentialBatch(
      batchId: CredentialBatchId,
      revocationLedgerData: LedgerData
  )(implicit database: Transactor[IO]): Unit = {
    CredentialBatchesDAO
      .revokeEntireBatch(batchId, revocationLedgerData)
      .transact(database)
      .unsafeRunSync()
    ()
  }

  private def revokeCredentials(
      batchId: CredentialBatchId,
      credentialHashes: List[SHA256Digest],
      revocationLedgerData: LedgerData
  )(implicit database: Transactor[IO]): Unit = {
    CredentialBatchesDAO
      .revokeCredentials(
        batchId,
        credentialHashes,
        revocationLedgerData
      )
      .transact(database)
      .unsafeRunSync()
  }

  private def revocationTime(batchId: CredentialBatchId, credentialHash: SHA256Digest)(implicit
      repository: CredentialBatchesRepository
  ): Option[LedgerData] = {
    repository
      .getCredentialRevocationTime(batchId, credentialHash)
      .value
      .futureValue
      .toOption
      .value
  }
}

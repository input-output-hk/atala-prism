package io.iohk.atala.prism.node.repositories

import java.time.Instant

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.DIDSuffix
import io.iohk.atala.prism.node.models.nodeState.CredentialBatchState
import io.iohk.atala.prism.node.operations.TimestampInfo
import io.iohk.atala.prism.node.repositories.daos.{CredentialBatchesDAO, DIDDataDAO}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO.CreateCredentialBatchData
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationLong

class CredentialBatchesRepositorySpec extends PostgresRepositorySpec {

  import CredentialBatchesRepositorySpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)
  implicit lazy val db = database

  private lazy implicit val repository = new CredentialBatchesRepository(database)

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

      val randomIssuerDIDSuffix = DIDSuffix(SHA256Digest.compute("did".getBytes()))
      val randomLastOperation = SHA256Digest.compute("lastOperation".getBytes())
      val randomMerkleRoot = MerkleRoot(SHA256Digest.compute("merkleRoot".getBytes()))
      val randomIssuedOnTime = TimestampInfo.dummyTime

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
        randomRevocationTime
      )

      revocationTime(randomBatchId, randomCredentialHash1) must be(Some(randomRevocationTime))
      revocationTime(randomBatchId, randomCredentialHash2) must be(Some(randomRevocationTime))
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
      val randomIssuerDIDSuffix = DIDSuffix(SHA256Digest.compute("did".getBytes()))
      val randomLastOperation = SHA256Digest.compute("lastOperation".getBytes())
      val randomMerkleRoot = MerkleRoot(SHA256Digest.compute("merkleRoot".getBytes()))
      val randomIssuedOnTime = TimestampInfo.dummyTime

      registerDID(randomIssuerDIDSuffix)

      createBatch(
        randomBatchId,
        randomLastOperation,
        randomIssuerDIDSuffix,
        randomMerkleRoot,
        randomIssuedOnTime
      )

      val expectedState = CredentialBatchState(
        batchId = randomBatchId,
        issuerDIDSuffix = randomIssuerDIDSuffix,
        merkleRoot = randomMerkleRoot,
        issuedOn = randomIssuedOnTime,
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
      val randomIssuerDIDSuffix = DIDSuffix(SHA256Digest.compute("did".getBytes()))
      val randomLastOperation = SHA256Digest.compute("lastOperation".getBytes())
      val randomMerkleRoot = MerkleRoot(SHA256Digest.compute("merkleRoot".getBytes()))
      val randomIssuedOnTime = TimestampInfo.dummyTime

      val randomRevocationTime = TimestampInfo(Instant.now(), 10, 100)

      registerDID(randomIssuerDIDSuffix)

      createBatch(
        randomBatchId,
        randomLastOperation,
        randomIssuerDIDSuffix,
        randomMerkleRoot,
        randomIssuedOnTime
      )

      revokeCredentialBatch(randomBatchId, randomRevocationTime)

      val expectedState = CredentialBatchState(
        batchId = randomBatchId,
        issuerDIDSuffix = randomIssuerDIDSuffix,
        merkleRoot = randomMerkleRoot,
        issuedOn = randomIssuedOnTime,
        revokedOn = Some(randomRevocationTime),
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
    DIDDataDAO
      .insert(didSuffix, lastOperation)
      .transact(database)
      .unsafeRunSync()
  }

  private def createBatch(
      batchId: CredentialBatchId,
      lastOperation: SHA256Digest,
      issuerDIDSuffix: DIDSuffix,
      merkleRoot: MerkleRoot,
      issuedOn: TimestampInfo
  )(implicit database: Transactor[IO]): Unit = {
    CredentialBatchesDAO
      .insert(
        CreateCredentialBatchData(
          batchId = batchId,
          lastOperation = lastOperation,
          issuerDIDSuffix = issuerDIDSuffix,
          merkleRoot = merkleRoot,
          issuedOn = issuedOn
        )
      )
      .transact(database)
      .unsafeRunSync()
  }

  private def revokeCredentialBatch(
      batchId: CredentialBatchId,
      revocationTime: TimestampInfo
  )(implicit database: Transactor[IO]): Unit = {
    CredentialBatchesDAO
      .revokeEntireBatch(batchId, revocationTime)
      .transact(database)
      .unsafeRunSync()
    ()
  }

  private def revokeCredentials(
      batchId: CredentialBatchId,
      credentialHashes: List[SHA256Digest],
      revocationTime: TimestampInfo
  )(implicit database: Transactor[IO]): Unit = {
    CredentialBatchesDAO
      .revokeCredentials(
        batchId,
        credentialHashes,
        revocationTime
      )
      .transact(database)
      .unsafeRunSync()
  }

  private def revocationTime(batchId: CredentialBatchId, credentialHash: SHA256Digest)(implicit
      repository: CredentialBatchesRepository
  ): Option[TimestampInfo] = {
    repository
      .getCredentialRevocationTime(batchId, credentialHash)
      .value
      .futureValue
      .toOption
      .value
  }
}

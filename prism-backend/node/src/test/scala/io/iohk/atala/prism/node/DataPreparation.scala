package io.iohk.atala.prism.node

import cats.effect.IO
import cats.implicits._
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey}
import io.iohk.atala.prism.node.models.nodeState.{DIDDataState, DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO.CreateCredentialBatchData
import io.iohk.atala.prism.node.repositories.daos.{CredentialBatchesDAO, DIDDataDAO, PublicKeysDAO}
import org.scalatest.OptionValues._

// This class collects useful methods to populate and query the node db that are
// not needed in the node production code, but are useful for tests.
// We also use these tests to test DAOs (note that the methods not present here are
// defined in corresponding repositories
object DataPreparation {

  // ***************************************
  // DIDs and keys
  // ***************************************

  def createDID(
      didData: DIDData,
      ledgerData: LedgerData
  )(implicit xa: Transactor[IO]): Unit = {
    val query = for {
      _ <- DIDDataDAO.insert(didData.didSuffix, didData.lastOperation, ledgerData)
      _ <- didData.keys.traverse((key: DIDPublicKey) => PublicKeysDAO.insert(key, ledgerData))
    } yield ()

    query
      .transact(xa)
      .unsafeRunSync()
  }

  def findByDidSuffix(didSuffix: DIDSuffix)(implicit xa: Transactor[IO]): DIDDataState = {
    val query = for {
      maybeLastOperation <- DIDDataDAO.getLastOperation(didSuffix)
      keys <- PublicKeysDAO.findAll(didSuffix)
    } yield DIDDataState(didSuffix, keys, maybeLastOperation.value)

    query
      .transact(xa)
      .unsafeRunSync()
  }

  def findKey(didSuffix: DIDSuffix, keyId: String)(implicit xa: Transactor[IO]): Option[DIDPublicKeyState] = {
    PublicKeysDAO
      .find(didSuffix, keyId)
      .transact(xa)
      .unsafeRunSync()
  }

  // ***************************************
  // Credential batches (slayer 0.3)
  // ***************************************

  def createBatch(
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

  def revokeCredentialBatch(
      batchId: CredentialBatchId,
      revocationLedgerData: LedgerData
  )(implicit database: Transactor[IO]): Unit = {
    CredentialBatchesDAO
      .revokeEntireBatch(batchId, revocationLedgerData)
      .transact(database)
      .unsafeRunSync()
    ()
  }

  def revokeCredentials(
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
}

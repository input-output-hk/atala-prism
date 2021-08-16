package io.iohk.atala.prism.node

import cats.effect.IO
import cats.implicits._
import com.google.protobuf.ByteString
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.models.Ledger
import io.iohk.atala.prism.node.cardano.{LAST_SYNCED_BLOCK_NO, LAST_SYNCED_BLOCK_TIMESTAMP}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus,
  AtalaOperationInfo,
  AtalaOperationStatus,
  DIDData,
  DIDPublicKey
}
import io.iohk.atala.prism.node.models.nodeState.{DIDDataState, DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.{issuingEcKey, masterEcKey}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.AtalaObjectCreateData
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO.CreateCredentialBatchData
import io.iohk.atala.prism.node.repositories.daos.{
  AtalaObjectTransactionSubmissionsDAO,
  AtalaObjectsDAO,
  AtalaOperationsDAO,
  CredentialBatchesDAO,
  DIDDataDAO,
  KeyValuesDAO,
  PublicKeysDAO
}
import org.scalatest.OptionValues._
import io.iohk.atala.prism.protos.{node_api, node_internal, node_models}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

import java.time.Instant

// This class collects useful methods to populate and query the node db that are
// not needed in the node production code, but are useful for tests.
// We also use these tests to test DAOs (note that the methods not present here are
// defined in corresponding repositories
object DataPreparation {
  val dummyTimestampInfo: TimestampInfo = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
  val exampleOperation: node_models.AtalaOperation = node_models.AtalaOperation(
    node_models.AtalaOperation.Operation.CreateDid(
      value = node_models.CreateDIDOperation(
        didData = Some(
          node_models.DIDData(
            id = "",
            publicKeys = List(
              node_models.PublicKey(
                "master",
                node_models.KeyUsage.MASTER_KEY,
                Some(
                  node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                ),
                None,
                node_models.PublicKey.KeyData.EcKeyData(masterEcKey)
              ),
              node_models
                .PublicKey(
                  "issuing",
                  node_models.KeyUsage.ISSUING_KEY,
                  Some(
                    node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                  ),
                  None,
                  node_models.PublicKey.KeyData.EcKeyData(issuingEcKey)
                )
            )
          )
        )
      )
    )
  )

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

  // ***************************************
  // Other useful methods
  // ***************************************

  def updateLastSyncedBlock(blockNo: Int, timestamp: Instant)(implicit xa: Transactor[IO]): Unit = {
    val query = for {
      _ <- KeyValuesDAO.upsert(KeyValuesDAO.KeyValue(LAST_SYNCED_BLOCK_NO, Some(blockNo.toString)))
      _ <-
        KeyValuesDAO.upsert(KeyValuesDAO.KeyValue(LAST_SYNCED_BLOCK_TIMESTAMP, Some(timestamp.toEpochMilli.toString)))
    } yield ()

    query
      .transact(xa)
      .unsafeRunSync()
  }

  def insertOperationStatuses(
      atalaOperations: List[SignedAtalaOperation],
      status: AtalaOperationStatus
  )(implicit xa: Transactor[IO]): (AtalaObjectId, List[AtalaOperationId]) = {
    val block = node_internal.AtalaBlock("1.0", atalaOperations)
    val obj = node_internal
      .AtalaObject(
        blockOperationCount = atalaOperations.size
      )
      .withBlockContent(block)
    val objBytes = obj.toByteArray
    val objId = AtalaObjectId.of(objBytes)
    val atalaOperationIds = atalaOperations.map(AtalaOperationId.of)
    val atalaOperationData = atalaOperationIds.map((_, objId, status))

    val query = for {
      insertObject <- AtalaObjectsDAO.insert(AtalaObjectCreateData(objId, objBytes))
      insertOperations <- AtalaOperationsDAO.insertMany(atalaOperationData)
    } yield (insertObject, insertOperations)

    query
      .transact(xa)
      .unsafeRunSync()
    (objId, atalaOperationIds)
  }

  def getOperationInfo(atalaOperationId: AtalaOperationId)(implicit xa: Transactor[IO]): Option[AtalaOperationInfo] = {
    AtalaOperationsDAO
      .getAtalaOperationInfo(atalaOperationId)
      .transact(xa)
      .unsafeRunSync()
  }

  def getOperationsCount()(implicit xa: Transactor[IO]): Int =
    sql"""
    |SELECT count(*)
    |FROM atala_operations
    """.stripMargin.query[Int].option.transact(xa).unsafeRunSync().getOrElse(0)

  def getPendingSubmissions()(implicit xa: Transactor[IO]): List[AtalaObjectTransactionSubmission] = {
    AtalaObjectTransactionSubmissionsDAO
      .getBy(
        olderThan = Instant.now(),
        status = AtalaObjectTransactionSubmissionStatus.Pending,
        ledger = Ledger.InMemory
      )
      .transact(xa)
      .unsafeRunSync()
  }

  def flushOperationsAndWaitConfirmation(
      nodeServiceStub: node_api.NodeServiceGrpc.NodeServiceBlockingStub,
      waitOperations: ByteString*
  ): Unit = {
    nodeServiceStub
      .flushOperationsBuffer(node_api.FlushOperationsBufferRequest())

    waitOperations.foreach { operationId =>
      val operationIdHex = AtalaOperationId.fromVectorUnsafe(operationId.toByteArray.toVector)
      while (!isOperationConfirmed(nodeServiceStub, operationId)) {
        println(s"Waiting until operation [$operationIdHex] is applied...")
      }
    }
  }

  def isOperationConfirmed(
      nodeServiceStub: node_api.NodeServiceGrpc.NodeServiceBlockingStub,
      operationId: ByteString
  ): Boolean = {
    // this try-catch is only the case for InMemory ledger, because InMemory
    // doesn't require ledger approval and executes operations immediately, so we can call getOperationInfo
    // after operation execution, but before the corresponding transaction recorded in the database.
    try {
      val status = nodeServiceStub
        .getOperationInfo(node_api.GetOperationInfoRequest(operationId))
        .operationStatus
      status.isConfirmedAndApplied || status.isConfirmedAndRejected
    } catch {
      case err: io.grpc.StatusRuntimeException if err.getMessage.contains("Unknown state of the operation") =>
        false
    }
  }
}

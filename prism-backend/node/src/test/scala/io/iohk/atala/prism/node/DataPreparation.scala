package io.iohk.atala.prism.node

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.google.protobuf.ByteString
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{MerkleRoot, Sha256, Sha256Digest}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models._
import io.iohk.atala.prism.node.cardano.{LAST_SYNCED_BLOCK_NO, LAST_SYNCED_BLOCK_TIMESTAMP}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.nodeState.{DIDDataState, DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.operations.ApplyOperationConfig
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.{issuingEcKeyData, masterEcKeyData}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.AtalaObjectCreateData
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO.CreateCredentialBatchData
import io.iohk.atala.prism.node.repositories.daos.{
  AtalaObjectTransactionSubmissionsDAO,
  AtalaObjectsDAO,
  AtalaOperationsDAO,
  ContextDAO,
  CredentialBatchesDAO,
  DIDDataDAO,
  KeyValuesDAO,
  PublicKeysDAO,
  ServicesDAO
}
import io.iohk.atala.prism.node.services.{BlockProcessingServiceSpec, ObjectManagementService, SubmissionService}
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_api, node_internal, node_models}
import org.scalatest.OptionValues._

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

// This class collects useful methods to populate and query the node db that are
// not needed in the node production code, but are useful for tests.
// We also use these tests to test DAOs (note that the methods not present here are
// defined in corresponding repositories
object DataPreparation {
  val dummyTimestampInfo: TimestampInfo = new TimestampInfo(0, 1, 0)
  lazy val dummyLedgerData: LedgerData = LedgerData(
    TransactionId
      .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
      .value,
    Ledger.InMemory,
    dummyTimestampInfo
  )
  val exampleOperation: node_models.AtalaOperation = node_models.AtalaOperation(
    node_models.AtalaOperation.Operation.CreateDid(
      value = node_models.CreateDIDOperation(
        didData = Some(
          node_models.CreateDIDOperation.DIDCreationData(
            publicKeys = List(
              node_models.PublicKey(
                "master",
                node_models.KeyUsage.MASTER_KEY,
                Some(
                  node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                ),
                None,
                node_models.PublicKey.KeyData.EcKeyData(masterEcKeyData)
              ),
              node_models
                .PublicKey(
                  "issuing",
                  node_models.KeyUsage.ISSUING_KEY,
                  Some(
                    node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                  ),
                  None,
                  node_models.PublicKey.KeyData.EcKeyData(issuingEcKeyData)
                )
            )
          )
        )
      )
    )
  )

  def moveToPendingAndSubmit(implicit
      submissionService: SubmissionService[IOWithTraceIdContext]
  ): Future[Either[NodeError, Int]] = {
    val query = for {
      _ <- submissionService.scheduledObjectsToPending
      _ <- submissionService.refreshTransactionStatuses()
      numE <- submissionService.submitPendingObjects()
    } yield numE
    query.run(TraceId.generateYOLO).unsafeToFuture()
  }

  def publishSingleOperationAndFlush(
      signedAtalaOperation: SignedAtalaOperation
  )(implicit
      objectManagementService: ObjectManagementService[IOWithTraceIdContext],
      submissionService: SubmissionService[IOWithTraceIdContext],
      executionContext: ExecutionContext
  ): Future[Either[NodeError, AtalaOperationId]] = {
    for {
      atalaOperationIdList <- objectManagementService
        .scheduleAtalaOperations(signedAtalaOperation)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
      atalaOperationIdE = atalaOperationIdList.head

      _ <- moveToPendingAndSubmit
    } yield atalaOperationIdE
  }

  def publishOperationsAndFlush(ops: SignedAtalaOperation*)(implicit
      objectManagementService: ObjectManagementService[IOWithTraceIdContext],
      submissionService: SubmissionService[IOWithTraceIdContext],
      executionContext: ExecutionContext
  ): Future[List[Either[NodeError, AtalaOperationId]]] = {
    for {
      ids <- objectManagementService
        .scheduleAtalaOperations(ops: _*)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()

      _ <- moveToPendingAndSubmit
    } yield ids
  }

  val dummyTime: TimestampInfo = new TimestampInfo(0, 1, 0)

  val dummyTimestamp: Instant =
    Instant.ofEpochMilli(dummyTime.getAtalaBlockTimestamp)
  val dummyABSequenceNumber: Int = dummyTime.getAtalaBlockSequenceNumber
  val dummyTransactionInfo: TransactionInfo =
    TransactionInfo(
      transactionId = TransactionId.from(Sha256.compute("id".getBytes).getValue).value,
      ledger = Ledger.InMemory,
      block = Some(
        BlockInfo(
          number = 1,
          timestamp = dummyTimestamp,
          index = dummyABSequenceNumber
        )
      )
    )
  val dummyPublicationInfo: PublicationInfo =
    PublicationInfo(dummyTransactionInfo, TransactionStatus.Pending)

  // ***************************************
  // DIDs and keys
  // ***************************************

  def createDID(
      didData: DIDData,
      ledgerData: LedgerData
  )(implicit xa: Transactor[IO]): Unit = {
    val query = for {
      _ <- DIDDataDAO.insert(
        didData.didSuffix,
        didData.lastOperation,
        ledgerData
      )
      _ <- didData.keys.traverse((key: DIDPublicKey) => PublicKeysDAO.insert(key, ledgerData))
      _ <- didData.services.traverse((service: DIDService) => ServicesDAO.insert(service, ledgerData))
      _ <- didData.context.traverse((contextStr: String) =>
        ContextDAO.insert(contextStr, didData.didSuffix, ledgerData)
      )
    } yield ()

    query
      .transact(xa)
      .unsafeRunSync()
  }

  def findByDidSuffix(
      didSuffix: DidSuffix
  )(implicit xa: Transactor[IO]): DIDDataState = {
    val query = for {
      maybeLastOperation <- DIDDataDAO.getLastOperation(didSuffix)
      keys <- PublicKeysDAO.findAll(didSuffix)
      services <- ServicesDAO.getAllActiveByDidSuffix(didSuffix)
      context <- ContextDAO.getAllActiveByDidSuffix(didSuffix)
    } yield DIDDataState(didSuffix, keys, services, context, maybeLastOperation.value)

    query
      .transact(xa)
      .unsafeRunSync()
  }

  def findKey(didSuffix: DidSuffix, keyId: String)(implicit
      xa: Transactor[IO]
  ): Option[DIDPublicKeyState] = {
    PublicKeysDAO
      .find(didSuffix, keyId)
      .transact(xa)
      .unsafeRunSync()
  }

  def revokeKey(didSuffix: DidSuffix, keyId: String, ledgerData: LedgerData)(implicit
      xa: Transactor[IO]
  ): Unit = {
    val _ = PublicKeysDAO
      .revoke(didSuffix, keyId, ledgerData)
      .transact(xa)
      .unsafeRunSync()
  }

  // ***************************************
  // Credential batches (slayer 0.3)
  // ***************************************

  def createBatch(
      batchId: CredentialBatchId,
      lastOperation: Sha256Digest,
      issuerDIDSuffix: DidSuffix,
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
      credentialHashes: List[Sha256Digest],
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

  def createBlock(
      signedOperation: node_models.SignedAtalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
  ): node_internal.AtalaBlock = {
    node_internal.AtalaBlock(operations = Seq(signedOperation))
  }

  def createBlock(
      signedOperations: List[node_models.SignedAtalaOperation]
  ): node_internal.AtalaBlock = {
    node_internal.AtalaBlock(operations = signedOperations)
  }

  def createAtalaObject(
      block: node_internal.AtalaBlock = createBlock(),
      opsCount: Int = 1
  ): node_internal.AtalaObject =
    node_internal
      .AtalaObject(
        blockOperationCount = opsCount
      )
      .withBlockContent(block)
      .withBlockByteLength(block.toByteArray.length)

  def setAtalaObjectTransactionSubmissionStatus(
      transaction: TransactionInfo,
      status: AtalaObjectTransactionSubmissionStatus
  )(implicit db: Transactor[IO]): Unit = {
    AtalaObjectTransactionSubmissionsDAO
      .updateStatus(transaction.ledger, transaction.transactionId, status)
      .transact(db)
      .unsafeRunSync()
    ()
  }

  def updateLastSyncedBlock(blockNo: Int, timestamp: Instant)(implicit
      xa: Transactor[IO]
  ): Unit = {
    val query = for {
      _ <- KeyValuesDAO.upsert(
        KeyValuesDAO.KeyValue(LAST_SYNCED_BLOCK_NO, Some(blockNo.toString))
      )
      _ <-
        KeyValuesDAO.upsert(
          KeyValuesDAO.KeyValue(
            LAST_SYNCED_BLOCK_TIMESTAMP,
            Some(timestamp.toEpochMilli.toString)
          )
        )
    } yield ()

    query
      .transact(xa)
      .unsafeRunSync()
  }

  def insertOperationStatuses(
      atalaOperations: List[SignedAtalaOperation],
      status: AtalaOperationStatus
  )(implicit xa: Transactor[IO]): (AtalaObjectId, List[AtalaOperationId]) = {
    val block = node_internal.AtalaBlock(atalaOperations)
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
      insertObject <- AtalaObjectsDAO.insert(
        AtalaObjectCreateData(objId, objBytes, AtalaObjectStatus.Scheduled)
      )
      insertOperations <- AtalaOperationsDAO.insertMany(atalaOperationData)
    } yield (insertObject, insertOperations)

    query
      .transact(xa)
      .unsafeRunSync()
    (objId, atalaOperationIds)
  }

  def getOperationInfo(
      atalaOperationId: AtalaOperationId
  )(implicit xa: Transactor[IO]): Option[AtalaOperationInfo] = {
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

  def getSubmissionsByStatus(
      status: AtalaObjectTransactionSubmissionStatus
  )(implicit xa: Transactor[IO]): List[AtalaObjectTransactionSubmission] = {
    AtalaObjectTransactionSubmissionsDAO
      .getBy(
        olderThan = Instant.now(),
        status = status,
        ledger = Ledger.InMemory
      )
      .transact(xa)
      .unsafeRunSync()
  }

  def waitConfirmation(
      nodeServiceStub: node_api.NodeServiceGrpc.NodeServiceBlockingStub,
      waitOperations: ByteString*
  ): Unit =
    waitOperations.foreach { operationId =>
      while (!isOperationConfirmed(nodeServiceStub, operationId)) {
        Thread.sleep(1000) // wait for one second
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

  val dummyApplyOperationConfig: ApplyOperationConfig = ApplyOperationConfig(DidSuffix(""))
}

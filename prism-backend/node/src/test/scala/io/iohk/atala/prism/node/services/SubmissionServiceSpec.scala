package io.iohk.atala.prism.node.services

import cats.effect.{ContextShift, IO}
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.{Ledger, TransactionDetails, TransactionId, TransactionStatus}
import io.iohk.atala.prism.node.cardano.models.AtalaObjectMetadata.estimateTxMetadataSize
import io.iohk.atala.prism.node.cardano.models.{CardanoWalletError, CardanoWalletErrorCode}
import io.iohk.atala.prism.node.models.AtalaObjectTransactionSubmissionStatus
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec
import io.iohk.atala.prism.node.{DataPreparation, PublicationInfo, UnderlyingLedger, cardano}
import io.iohk.atala.prism.node.DataPreparation._
import io.iohk.atala.prism.node.repositories.{
  AtalaObjectsTransactionsRepository,
  AtalaOperationsRepository,
  KeyValuesRepository,
  ProtocolVersionRepository
}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.utils.IOUtils._
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.mockito.scalatest.{MockitoSugar, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._
import tofu.logging.Logs

import scala.concurrent.duration._
import java.time.Duration
import scala.concurrent.{ExecutionContext, Future}

object SubmissionServiceSpec {}

class SubmissionServiceSpec
    extends AtalaWithPostgresSpec
    with MockitoSugar
    with ResetMocksAfterEachTest
    with BeforeAndAfterEach {

  private implicit val ce: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
  private val logs = Logs.withContext[IO, IOWithTraceIdContext]
  private val ledger: UnderlyingLedger = mock[UnderlyingLedger]
  private val atalaOperationsRepository: AtalaOperationsRepository[IOWithTraceIdContext] =
    AtalaOperationsRepository.unsafe(dbLiftedToTraceIdIO, logs)
  private val atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[IOWithTraceIdContext] =
    AtalaObjectsTransactionsRepository.unsafe(dbLiftedToTraceIdIO, logs)
  private val keyValuesRepository: KeyValuesRepository[IOWithTraceIdContext] =
    KeyValuesRepository.unsafe(dbLiftedToTraceIdIO, logs)
  private val protocolVersionRepository: ProtocolVersionRepository[IOWithTraceIdContext] =
    ProtocolVersionRepository.unsafe(
      dbLiftedToTraceIdIO,
      logs
    )
  private val blockProcessing: BlockProcessingService =
    mock[BlockProcessingService]

  private implicit lazy val submissionService: SubmissionService[IOWithTraceIdContext] =
    SubmissionService.unsafe(
      ledger,
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      logs = logs
    )

  private val config: SubmissionSchedulingService.Config =
    SubmissionSchedulingService.Config(
      ledgerPendingTransactionTimeout = Duration.ZERO,
      transactionRetryPeriod = 1.hour,
      operationSubmissionPeriod = 1.hour
    )

  private implicit lazy val objectManagementService: ObjectManagementService[IOWithTraceIdContext] =
    ObjectManagementService.unsafe(
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      keyValuesRepository,
      protocolVersionRepository,
      blockProcessing,
      dbLiftedToTraceIdIO,
      logs
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    doReturn(Ledger.InMemory).when(ledger).getType
    ()
  }

  "SubmissionService.submitReceivedObjects" should {
    "merge several operations in one transaction while submitting" in {
      val (atalaObjects, atalaObjectsMerged, publications, ops) =
        setUpMultipleOperationsPublishing(numOps = 40)

      atalaObjectsMerged.zip(publications.drop(atalaObjects.size)).foreach { case (atalaObject, publicationInfo) =>
        doReturn(Future.successful(Right(publicationInfo)))
          .when(ledger)
          .publish(atalaObject)
        mockTransactionStatus(
          publicationInfo.transaction.transactionId,
          TransactionStatus.Pending
        )
      }

      scheduleOpsForBatching(ops)
      submissionService
        .submitReceivedObjects()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .toOption
        .nonEmpty must be(
        true
      )

      verify(ledger, times(2))
        .publish(*) // publish only merged objects

      assert(
        estimateTxMetadataSize(
          atalaObjectsMerged.head
        ) < cardano.TX_METADATA_MAX_SIZE
      )
      assert(
        estimateTxMetadataSize(
          atalaObjectsMerged(1)
        ) < cardano.TX_METADATA_MAX_SIZE
      )
      assert(
        estimateTxMetadataSize(
          atalaObjectsMerged(1)
        ) > cardano.TX_METADATA_MAX_SIZE / 2
      ) // check that merged object is big enough

      DataPreparation
        .getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.Pending)
        .size must be(2)
      val notPublishedObjects =
        AtalaObjectsDAO.getNotPublishedObjectInfos
          .transact(database)
          .unsafeToFuture()
          .futureValue
      notPublishedObjects.size must be(0) // no pending objects
    }

    "record published operations even if others are failed" in {
      val (_, atalaObjectsMerged, publications, ops) =
        setUpMultipleOperationsPublishing(numOps = 40)

      // first publishing is failed
      doReturn(
        Future.successful(
          Left(
            CardanoWalletError(
              "UtxoTooSmall",
              CardanoWalletErrorCode.UtxoTooSmall
            )
          )
        )
      )
        .when(ledger)
        .publish(atalaObjectsMerged.head)

      // second is ok
      doReturn(Future.successful(Right(publications.last)))
        .when(ledger)
        .publish(atalaObjectsMerged.last)
      mockTransactionStatus(
        publications.last.transaction.transactionId,
        TransactionStatus.Pending
      )

      scheduleOpsForBatching(ops)
      submissionService
        .submitReceivedObjects()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .toOption
        .nonEmpty must be(
        true
      )

      verify(ledger, times(2))
        .publish(*) // publish only merged objects

      DataPreparation
        .getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.Pending)
        .size must be(1)
      val notPublishedObjects =
        AtalaObjectsDAO.getNotPublishedObjectInfos
          .transact(database)
          .unsafeToFuture()
          .futureValue
      notPublishedObjects.size must be(1) // no pending objects

      // after publication second transaction becomes InLedger
      mockTransactionStatus(
        publications.last.transaction.transactionId,
        TransactionStatus.InLedger
      )

      // publishing the first operation while retrying becomes ok
      doReturn(Future.successful(Right(publications.dropRight(1).last)))
        .when(ledger)
        .publish(atalaObjectsMerged.head)
      mockTransactionStatus(
        publications.dropRight(1).last.transaction.transactionId,
        TransactionStatus.Pending
      )

      // updates statuses for inLedger submissions
      // note that we're not resubmitting the first object here since it wasn't published at all
      submissionService
        .retryOldPendingTransactions(config.ledgerPendingTransactionTimeout)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      DataPreparation
        .getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.Pending)
        .size must be(0)

      // resubmits object1
      submissionService
        .submitReceivedObjects()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .toOption
        .nonEmpty must be(
        true
      )
      val notPublishedObjects2 =
        AtalaObjectsDAO.getNotPublishedObjectInfos
          .transact(database)
          .unsafeToFuture()
          .futureValue
      notPublishedObjects2.size must be(0) // no pending objects
    }
  }

  "SubmissionService.retryOldPendingTransactions" should {
    val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
    val atalaObject = createAtalaObject(block = createBlock(atalaOperation))

    "not delete already published transaction" in {
      val (atalaObjects, atalaObjectsMerged, publications, ops) =
        setUpMultipleOperationsPublishing(numOps = 3, numPubsAdditional = 1)
      val opInLedger = BlockProcessingServiceSpec.signOperation(
        DataPreparation.exampleOperation,
        s"master${ops.size}",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val objInLedger = createAtalaObject(block = createBlock(opInLedger))

      (atalaObjects ++ atalaObjectsMerged :+ objInLedger)
        .zip(publications)
        .foreach { case (atalaObject, publicationInfo) =>
          doReturn(Future.successful(Right(publicationInfo)))
            .when(ledger)
            .publish(atalaObject)
          mockTransactionStatus(
            publicationInfo.transaction.transactionId,
            TransactionStatus.Pending
          )
        }
      publications.take(atalaObjects.size).foreach { publicationInfo =>
        doReturn(Future.successful(Right(())))
          .when(ledger)
          .deleteTransaction(publicationInfo.transaction.transactionId)
      }
      val inLedgerTransactionId =
        publications
          .drop(atalaObjects.size + atalaObjectsMerged.size)
          .head
          .transaction
          .transactionId
      doReturn(
        Future.successful(
          Left(
            CardanoWalletError(
              "Too late",
              CardanoWalletErrorCode.TransactionAlreadyInLedger
            )
          )
        )
      ).when(ledger).deleteTransaction(inLedgerTransactionId)

      publishOpsSequentially(ops :+ opInLedger)

      DataPreparation
        .getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.InLedger)
        .size must be(0)
      submissionService
        .retryOldPendingTransactions(config.ledgerPendingTransactionTimeout)
        .run(TraceId.generateYOLO)
        .unsafeRunSync() must be(1)

      val inLedgerTxs = DataPreparation.getSubmissionsByStatus(
        AtalaObjectTransactionSubmissionStatus.InLedger
      )
      inLedgerTxs.size must be(1)
      inLedgerTxs.head.transactionId must be(inLedgerTransactionId)
    }

    "ignore in-ledger transactions" in {
      doReturn(Future.successful(Right(dummyPublicationInfo)))
        .when(ledger)
        .publish(*)
      // Publish once and update status
      publishSingleOperationAndFlush(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(
        dummyPublicationInfo.transaction,
        AtalaObjectTransactionSubmissionStatus.InLedger
      )

      submissionService
        .retryOldPendingTransactions(config.ledgerPendingTransactionTimeout)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore deleted transactions" in {
      doReturn(Future.successful(Right(dummyPublicationInfo)))
        .when(ledger)
        .publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(
        dummyPublicationInfo.transaction,
        AtalaObjectTransactionSubmissionStatus.Deleted
      )

      submissionService
        .retryOldPendingTransactions(config.ledgerPendingTransactionTimeout)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore other ledger's transactions" in {
      doReturn(Future.successful(Right(dummyPublicationInfo)))
        .when(ledger)
        .publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      // Simulate the service is restarted with a new ledger type
      doReturn(Ledger.CardanoTestnet).when(ledger).getType

      submissionService
        .retryOldPendingTransactions(config.ledgerPendingTransactionTimeout)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "retry old pending transactions" in {
      val dummyTransactionId2 =
        TransactionId.from(Sha256.compute("id2".getBytes).getValue).value
      val dummyTransactionInfo2 =
        dummyTransactionInfo.copy(transactionId = dummyTransactionId2)
      val dummyPublicationInfo2 =
        dummyPublicationInfo.copy(transaction = dummyTransactionInfo2)
      // Return dummyTransactionInfo and then dummyTransactionInfo2
      doReturn(
        Future.successful(Right(dummyPublicationInfo)),
        Future.successful(Right(dummyPublicationInfo2))
      )
        .when(ledger)
        .publish(*)
      doReturn(Future.successful(Right(())))
        .when(ledger)
        .deleteTransaction(dummyTransactionInfo.transactionId)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      mockTransactionStatus(
        dummyTransactionInfo.transactionId,
        TransactionStatus.Pending
      )

      submissionService
        .retryOldPendingTransactions(config.ledgerPendingTransactionTimeout)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published twice and deleted the first one
      verify(ledger, times(2)).publish(atalaObject)
      verify(ledger).deleteTransaction(dummyTransactionInfo.transactionId)
    }

    "merge several operations in one transaction while retrying" in {
      val (atalaObjects, atalaObjectsMerged, publications, ops) =
        setUpMultipleOperationsPublishing(numOps = 40)

      (atalaObjects ++ atalaObjectsMerged).zip(publications).foreach { case (atalaObject, publicationInfo) =>
        doReturn(Future.successful(Right(publicationInfo)))
          .when(ledger)
          .publish(atalaObject)
        mockTransactionStatus(
          publicationInfo.transaction.transactionId,
          TransactionStatus.Pending
        )
      }
      publications.dropRight(atalaObjectsMerged.size).foreach { publicationInfo =>
        doReturn(Future.successful(Right(())))
          .when(ledger)
          .deleteTransaction(publicationInfo.transaction.transactionId)
      }

      // publish operations sequentially because we want to preserve the order by timestamps
      publishOpsSequentially(ops)

      submissionService
        .retryOldPendingTransactions(config.ledgerPendingTransactionTimeout)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      verify(ledger, times(atalaObjects.size + 2))
        .publish(
          *
        ) // publish transactions for initial objects and for two new objects

      assert(
        estimateTxMetadataSize(
          atalaObjectsMerged.head
        ) < cardano.TX_METADATA_MAX_SIZE
      )
      assert(
        estimateTxMetadataSize(
          atalaObjectsMerged(1)
        ) < cardano.TX_METADATA_MAX_SIZE
      )
      assert(
        estimateTxMetadataSize(
          atalaObjectsMerged(1)
        ) > cardano.TX_METADATA_MAX_SIZE / 2
      ) // check that merged object is big enough

      DataPreparation
        .getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.Pending)
        .size must be(2)

      val notPublishedObjects =
        AtalaObjectsDAO.getNotPublishedObjectInfos
          .transact(database)
          .unsafeToFuture()
          .futureValue
      notPublishedObjects.size must be(0) // no pending objects
    }

    "not retry new pending transactions" in {
      // Use a service that does have a 10-minute timeout for pending transactions
      doReturn(Future.successful(Right(dummyPublicationInfo)))
        .when(ledger)
        .publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      mockTransactionStatus(
        dummyTransactionInfo.transactionId,
        TransactionStatus.Pending
      )

      submissionService
        .retryOldPendingTransactions(Duration.ofMinutes(10))
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(
        dummyTransactionInfo.transactionId
      )
    }

    "not retry in-ledger transactions" in {
      doReturn(Future.successful(Right(dummyPublicationInfo)))
        .when(ledger)
        .publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      mockTransactionStatus(
        dummyTransactionInfo.transactionId,
        TransactionStatus.InLedger
      )

      submissionService
        .retryOldPendingTransactions(config.ledgerPendingTransactionTimeout)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(
        dummyTransactionInfo.transactionId
      )
    }
  }

  private def scheduleOpsForBatching(ops: List[SignedAtalaOperation]): Unit =
    ops.zipWithIndex.foreach { case (atalaOperation, index) =>
      withClue(s"scheduling operation #$index") {
        objectManagementService
          .scheduleSingleAtalaOperation(atalaOperation)
          .run(TraceId.generateYOLO)
          .unsafeToFuture()
          .futureValue
      }
    }

  private def publishOpsSequentially(ops: List[SignedAtalaOperation]): Unit =
    ops.zipWithIndex.foreach { case (atalaOperation, index) =>
      withClue(s"publishing operation #$index and flushing") {
        publishSingleOperationAndFlush(atalaOperation).futureValue
      }
    }

  private def setUpMultipleOperationsPublishing(
      numOps: Int,
      numPubsAdditional: Int = 0
  ): (
      List[node_internal.AtalaObject],
      List[node_internal.AtalaObject],
      List[PublicationInfo],
      List[SignedAtalaOperation]
  ) = {
    val atalaOperations = (0 until numOps).toList.map { masterId =>
      BlockProcessingServiceSpec.signOperation(
        DataPreparation.exampleOperation,
        s"master$masterId",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
    }

    val atalaObjects = atalaOperations.map { op =>
      createAtalaObject(block = createBlock(op))
    }

    // Calculate atala objects merged in a naive way
    var accOps = List.empty[SignedAtalaOperation]
    var oldObj: node_internal.AtalaObject = null
    val atalaObjectsMerged =
      collection.mutable.ArrayBuffer.empty[node_internal.AtalaObject]
    atalaOperations.reverse.foreach { op =>
      val nextAccOps = op +: accOps
      val curObj = createAtalaObject(
        block = node_internal.AtalaBlock(operations = nextAccOps),
        opsCount = nextAccOps.size
      )

      if (estimateTxMetadataSize(curObj) >= cardano.TX_METADATA_MAX_SIZE) {
        assert(oldObj != null)
        atalaObjectsMerged.append(oldObj)
        oldObj = createAtalaObject(block = node_internal.AtalaBlock(operations = List(op)))
        accOps = List(op)
      } else {
        oldObj = curObj
        accOps = nextAccOps
      }
    }
    if (oldObj != null) atalaObjectsMerged.append(oldObj)

    val dummyTransactionIds =
      (0 until (atalaOperations.size + atalaObjectsMerged.size + numPubsAdditional))
        .map { index =>
          TransactionId
            .from(Sha256.compute(s"id$index".getBytes).getValue)
            .value
        }
    val dummyTransactionInfos = dummyTransactionIds.map { transactionId =>
      dummyTransactionInfo.copy(transactionId = transactionId)
    }
    val dummyPublicationInfos = dummyTransactionInfos.map { transactionInfo =>
      dummyPublicationInfo.copy(transaction = transactionInfo)
    }
    (
      atalaObjects,
      atalaObjectsMerged.reverse.toList,
      dummyPublicationInfos.toList,
      atalaOperations
    )
  }

  def mockTransactionStatus(
      transactionId: TransactionId,
      status: TransactionStatus
  ): Unit = {
    doReturn(
      Future.successful(Right(TransactionDetails(transactionId, status)))
    )
      .when(ledger)
      .getTransactionDetails(transactionId)
    ()
  }
}

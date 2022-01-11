package io.iohk.atala.prism.node.services

import cats.data.ReaderT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
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
import org.mockito.scalatest.{MockitoSugar, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._
import tofu.logging.Logs

object SubmissionServiceSpec {}

class SubmissionServiceSpec
    extends AtalaWithPostgresSpec
    with MockitoSugar
    with ResetMocksAfterEachTest
    with BeforeAndAfterEach {

  private val logs = Logs.withContext[IO, IOWithTraceIdContext]
  private val ledger: UnderlyingLedger[IOWithTraceIdContext] =
    mock[UnderlyingLedger[IOWithTraceIdContext]]
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

    resetAll()
    doReturn(Ledger.InMemory).when(ledger).getType
    ()
  }

  "SubmissionService.submitReceivedObjects" should {
    "merge several operations in one transaction while submitting" in {
      val (atalaObjects, atalaObjectsMerged, publications, ops) =
        setUpMultipleOperationsPublishing(numOps = 40)

      atalaObjectsMerged.zip(publications.drop(atalaObjects.size)).foreach { case (atalaObject, publicationInfo) =>
        doReturn(
          ReaderT
            .pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
              Right(publicationInfo)
            )
        )
          .when(ledger)
          .publish(atalaObject)
        mockTransactionStatus(
          publicationInfo.transaction.transactionId,
          TransactionStatus.Pending
        )
      }

      objectManagementService
        .scheduleAtalaOperations(ops: _*)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue

      DataPreparation.moveToPendingAndSubmit.futureValue must be(Right(atalaObjectsMerged.size))

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

      // the publication of the first object is failed
      doReturn(
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
          Left(
            CardanoWalletError(
              "UtxoTooSmall",
              CardanoWalletErrorCode.UtxoTooSmall
            )
          )
        )
      ).when(ledger)
        .publish(atalaObjectsMerged.head)

      // the publication of the second object is successful
      doReturn(
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
          Right(publications.last)
        )
      )
        .when(ledger)
        .publish(atalaObjectsMerged.last)
      // status of the second publication is Pending
      mockTransactionStatus(
        publications.last.transaction.transactionId,
        TransactionStatus.Pending
      )

      objectManagementService
        .scheduleAtalaOperations(ops: _*)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue

      DataPreparation.moveToPendingAndSubmit.futureValue must be(Right(1))

      verify(ledger, times(2))
        .publish(*) // publish merged objects

      DataPreparation
        .getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.Pending)
        .size must be(1)

      submissionService.scheduledObjectsToPending.run(TraceId.generateYOLO).unsafeRunSync() must be(Right(0))

      val notPublishedObjects =
        AtalaObjectsDAO.getNotPublishedObjectInfos
          .transact(database)
          .unsafeToFuture()
          .futureValue
      notPublishedObjects.size must be(1)

      // after publication second transaction becomes InLedger
      mockTransactionStatus(
        publications.last.transaction.transactionId,
        TransactionStatus.InLedger
      )

      // publishing the first operation while retrying becomes ok
      doReturn(
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
          Right(publications.dropRight(1).last)
        )
      ).when(ledger)
        .publish(atalaObjectsMerged.head)
      mockTransactionStatus(
        publications.dropRight(1).last.transaction.transactionId,
        TransactionStatus.Pending
      )

      // updates statuses for inLedger submissions
      // note that we're not resubmitting the first object here since it wasn't published at all
      submissionService
        .refreshTransactionStatuses()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      DataPreparation
        .getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.Pending)
        .size must be(0)

      // resubmits object1
      submissionService
        .submitPendingObjects()
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

  "SubmissionService.refreshTransactionStatuses" should {
    val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
    val atalaObject = createAtalaObject(block = createBlock(atalaOperation))

    "ignore in-ledger transactions" in {
      doReturn(
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
          Right(dummyPublicationInfo)
        )
      )
        .when(ledger)
        .publish(*)
      // Publish once and update status
      publishSingleOperationAndFlush(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(
        dummyPublicationInfo.transaction,
        AtalaObjectTransactionSubmissionStatus.InLedger
      )

      submissionService
        .refreshTransactionStatuses()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore deleted transactions" in {
      doReturn(
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
          Right(dummyPublicationInfo)
        )
      )
        .when(ledger)
        .publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(
        dummyPublicationInfo.transaction,
        AtalaObjectTransactionSubmissionStatus.Deleted
      )

      submissionService
        .refreshTransactionStatuses()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore other ledger's transactions" in {
      doReturn(
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
          Right(dummyPublicationInfo)
        )
      )
        .when(ledger)
        .publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      // Simulate the service is restarted with a new ledger type
      doReturn(Ledger.CardanoTestnet).when(ledger).getType

      submissionService
        .refreshTransactionStatuses()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "refresh old transactions and resubmit" in {
      val dummyTransactionId2 =
        TransactionId.from(Sha256.compute("id2".getBytes).getValue).value
      val dummyTransactionInfo2 =
        dummyTransactionInfo.copy(transactionId = dummyTransactionId2)
      val dummyPublicationInfo2 =
        dummyPublicationInfo.copy(transaction = dummyTransactionInfo2)
      // Return dummyTransactionInfo and then dummyTransactionInfo2
      doReturn(
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
          Right(dummyPublicationInfo)
        ),
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
          Right(dummyPublicationInfo2)
        )
      ).when(ledger)
        .publish(*)
      doReturn(
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, Unit]](Right(()))
      )
        .when(ledger)
        .deleteTransaction(dummyTransactionInfo.transactionId)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      mockTransactionStatus(
        dummyTransactionInfo.transactionId,
        TransactionStatus.Expired
      )

      DataPreparation.moveToPendingAndSubmit.futureValue
      // It should have published twice and deleted the first one
      verify(ledger, times(2)).publish(atalaObject)
      verify(ledger).deleteTransaction(dummyTransactionInfo.transactionId)
    }

    "merge several operations in one transaction while resubmitting expired transactions" in {
      val (atalaObjects, atalaObjectsMerged, publications, ops) =
        setUpMultipleOperationsPublishing(numOps = 40)

      // `publish` and `getTransactionDetails` should be called on every input object and on every merged object
      (atalaObjects ++ atalaObjectsMerged).zip(publications).foreach { case (atalaObject, publicationInfo) =>
        doReturn(
          ReaderT
            .pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
              Right(publicationInfo)
            )
        )
          .when(ledger)
          .publish(atalaObject)
        mockTransactionStatus(
          publicationInfo.transaction.transactionId,
          TransactionStatus.Pending
        )
      }

      // all initial objects should be deleted because corresponding transactions are expired on Cardano wallet side
      publications.take(atalaObjects.size).foreach { publicationInfo =>
        doReturn(
          ReaderT.pure[IO, TraceId, Either[CardanoWalletError, Unit]](
            Right(())
          )
        )
          .when(ledger)
          .deleteTransaction(publicationInfo.transaction.transactionId)
      }

      // publish operations sequentially because we want to preserve the order by timestamps
      publishOpsSequentially(ops)

      // after all operations are sent to the Cardano wallet, the corresponding transactions become Expired
      publications.take(atalaObjects.size).foreach { publicationInfo =>
        mockTransactionStatus(
          publicationInfo.transaction.transactionId,
          TransactionStatus.Expired
        )
      }

      // delete all `Expired` transactions, so that the corresponding objects become in status `Pending` again
      submissionService
        .refreshTransactionStatuses()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // submit two objects containing 40 pending operations in total
      submissionService
        .submitPendingObjects()
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
      doReturn(
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
          Right(dummyPublicationInfo)
        )
      )
        .when(ledger)
        .publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      mockTransactionStatus(
        dummyTransactionInfo.transactionId,
        TransactionStatus.Pending
      )

      submissionService
        .refreshTransactionStatuses()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(
        dummyTransactionInfo.transactionId
      )
    }

    "not retry in-ledger transactions" in {
      doReturn(
        ReaderT.pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
          Right(dummyPublicationInfo)
        )
      )
        .when(ledger)
        .publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      mockTransactionStatus(
        dummyTransactionInfo.transactionId,
        TransactionStatus.InLedger
      )

      submissionService
        .refreshTransactionStatuses()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(
        dummyTransactionInfo.transactionId
      )
    }
  }

  private def publishOpsSequentially(ops: List[SignedAtalaOperation]): Unit =
    ops.zipWithIndex.foreach { case (atalaOperation, index) =>
      withClue(s"publishing operation #$index and flushing") {
        publishSingleOperationAndFlush(atalaOperation).futureValue
      }
    }

  // Utility method to create new samples of operations and the corresponding publication data
  // It takes amount of operations required and amount of additional publication structures
  private def setUpMultipleOperationsPublishing(
      numOps: Int,
      numPubsAdditional: Int = 0
  ): (
      List[node_internal.AtalaObject],
      List[node_internal.AtalaObject],
      List[PublicationInfo],
      List[SignedAtalaOperation]
  ) = {
    // Create a list of `numOps` operations
    val atalaOperations = (0 until numOps).toList.map { masterId =>
      BlockProcessingServiceSpec.signOperation(
        DataPreparation.exampleOperation,
        s"master$masterId",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
    }

    // Create a new AtalaObject for every operation
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

    // Create a new publication information for every object
    // `atalaOperations.size` is amount of initial objects
    // `atalaObjectsMerged.size` is amount of merged objects
    // `numPubsAdditional` is amount of additional publications needed
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
      ReaderT.pure[IO, TraceId, Either[CardanoWalletError, TransactionDetails]](
        Right(TransactionDetails(transactionId, status))
      )
    ).when(ledger).getTransactionDetails(transactionId)
    ()
  }
}

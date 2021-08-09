package io.iohk.atala.prism.node.services

import doobie.free.connection
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.kotlin.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.models.{
  BlockInfo,
  Ledger,
  TransactionDetails,
  TransactionId,
  TransactionInfo,
  TransactionStatus
}
import io.iohk.atala.prism.node.cardano
import io.iohk.atala.prism.node.cardano.models.AtalaObjectMetadata.estimateTxMetadataSize
import io.iohk.atala.prism.node.cardano.models.{CardanoWalletError, CardanoWalletErrorCode}
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectInfo,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus,
  AtalaOperationStatus
}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectTransactionSubmissionsDAO, AtalaObjectsDAO}
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.{DataPreparation, PublicationInfo, UnderlyingLedger}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_internal, node_models}
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.mockito
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{MockitoSugar, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures

import java.time.{Duration, Instant}
import scala.concurrent.Future

object ObjectManagementServiceSpec {
  private val newKeysPairs = List.fill(10) { EC.generateKeyPair() }

  val exampleOperations: Seq[node_models.AtalaOperation] = newKeysPairs.zipWithIndex.map {
    case (keyPair: ECKeyPair, i) =>
      BlockProcessingServiceSpec.createDidOperation.update(_.createDid.didData.publicKeys.modify { keys =>
        keys :+ node_models.PublicKey(
          id = s"key$i",
          usage = node_models.KeyUsage.AUTHENTICATION_KEY,
          keyData = node_models.PublicKey.KeyData.EcKeyData(
            CreateDIDOperationSpec.protoECKeyFromPublicKey(keyPair.getPublicKey)
          )
        )
      })
  }

  val exampleSignedOperations: Seq[node_models.SignedAtalaOperation] = exampleOperations.map { operation =>
    BlockProcessingServiceSpec.signOperation(operation, "master", CreateDIDOperationSpec.masterKeys.getPrivateKey)
  }
}

class ObjectManagementServiceSpec
    extends AtalaWithPostgresSpec
    with MockitoSugar
    with ResetMocksAfterEachTest
    with BeforeAndAfterEach {
  private val ledger: UnderlyingLedger = mock[UnderlyingLedger]
  private val blockProcessing: BlockProcessingService = mock[BlockProcessingService]

  private lazy val objectManagementService =
    ObjectManagementService(
      ObjectManagementService.Config(ledgerPendingTransactionTimeout = Duration.ZERO),
      ledger,
      blockProcessing
    )

  private val dummyTime = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)

  private val dummyTimestamp = dummyTime.atalaBlockTimestamp
  private val dummyABSequenceNumber = dummyTime.atalaBlockSequenceNumber
  private val dummyTransactionInfo =
    TransactionInfo(
      transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).getValue).value,
      ledger = Ledger.InMemory,
      block = Some(BlockInfo(number = 1, timestamp = dummyTimestamp, index = dummyABSequenceNumber))
    )
  private val dummyPublicationInfo = PublicationInfo(dummyTransactionInfo, TransactionStatus.Pending)

  override def beforeEach(): Unit = {
    super.beforeEach()

    doReturn(Ledger.InMemory).when(ledger).getType
    ()
  }

  "ObjectManagementService.publishAtalaOperation" should {
    "update status to received when operation was received, but haven't processed yet" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId = BlockProcessingServiceSpec.signedCreateDidOperationId
      val returnedOperationId = publishSingleOperationAndFlush(atalaOperation).futureValue
      returnedOperationId mustBe atalaOperationId

      val atalaOperationInfo = objectManagementService.getOperationInfo(atalaOperationId).futureValue.value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(Some(AtalaObjectTransactionSubmissionStatus.Pending))
      atalaOperationInfo.transactionId.value must be(dummyPublicationInfo.transaction.transactionId)
    }

    "ignore publishing duplicate operation" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId = BlockProcessingServiceSpec.signedCreateDidOperationId
      val returnedAtalaOperation = publishSingleOperationAndFlush(atalaOperation).futureValue
      returnedAtalaOperation mustBe atalaOperationId

      ScalaFutures.whenReady(publishSingleOperationAndFlush(atalaOperation).failed) { err =>
        err mustBe a[DuplicateAtalaBlock]
      }

      val atalaOperationInfo = objectManagementService.getOperationInfo(atalaOperationId).futureValue.value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(Some(AtalaObjectTransactionSubmissionStatus.Pending))
      atalaOperationInfo.transactionId.value must be(dummyPublicationInfo.transaction.transactionId)
    }

    "ignore publishing duplicate operation in the same block" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId = BlockProcessingServiceSpec.signedCreateDidOperationId

      val opIds = publishOperationsAndFlush(atalaOperation, atalaOperation).futureValue

      opIds.size mustBe 2
      opIds.head mustBe atalaOperationId
      opIds.last mustBe atalaOperationId

      val atalaOperationInfo = objectManagementService.getOperationInfo(atalaOperationId).futureValue.value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(Some(AtalaObjectTransactionSubmissionStatus.Pending))
    }

    "put block content onto the ledger when supported" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)

      val returnedOperationId =
        publishSingleOperationAndFlush(BlockProcessingServiceSpec.signedCreateDidOperation).futureValue

      returnedOperationId mustBe BlockProcessingServiceSpec.signedCreateDidOperationId
      // Verify published AtalaObject
      val atalaObjectCaptor = ArgCaptor[node_internal.AtalaObject]
      verify(ledger).publish(atalaObjectCaptor)
      val atalaObject = atalaObjectCaptor.value
      val atalaBlock = atalaObject.blockContent.value
      atalaBlock.operations must contain theSameElementsAs Seq(BlockProcessingServiceSpec.signedCreateDidOperation)
      // Verify transaction submission
      val transactionSubmissions = queryTransactionSubmissions(AtalaObjectTransactionSubmissionStatus.Pending)
      transactionSubmissions.size mustBe 1

      val operationInfo = objectManagementService.getOperationInfo(returnedOperationId).futureValue.value
      operationInfo.operationId must be(returnedOperationId)
      operationInfo.transactionSubmissionStatus.value must be(AtalaObjectTransactionSubmissionStatus.Pending)
      operationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      operationInfo.transactionId.value must be(dummyPublicationInfo.transaction.transactionId)
    }

    "record immediate in-ledger transactions" in {
      val inLedgerPublication = dummyPublicationInfo.copy(status = TransactionStatus.InLedger)
      doReturn(Future.successful(Right(inLedgerPublication))).when(ledger).publish(*)

      val returnedOperationId =
        publishSingleOperationAndFlush(BlockProcessingServiceSpec.signedCreateDidOperation).futureValue

      returnedOperationId must be(BlockProcessingServiceSpec.signedCreateDidOperationId)

      // Verify transaction submission
      val transactionSubmissions = queryTransactionSubmissions(AtalaObjectTransactionSubmissionStatus.InLedger)
      transactionSubmissions.size mustBe 1

      val operationInfo = objectManagementService.getOperationInfo(returnedOperationId).futureValue.value
      operationInfo.operationId must be(returnedOperationId)
      operationInfo.transactionSubmissionStatus.value must be(AtalaObjectTransactionSubmissionStatus.InLedger)
      operationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      operationInfo.transactionId.value must be(inLedgerPublication.transaction.transactionId)
    }

    "merge several operations in one transaction while submitting" in {
      val (atalaObjects, atalaObjectsMerged, publications, ops) = setUpMultipleOperationsPublishing(numOps = 40)

      atalaObjectsMerged.zip(publications.drop(atalaObjects.size)).foreach {
        case (atalaObject, publicationInfo) =>
          doReturn(Future.successful(Right(publicationInfo))).when(ledger).publish(atalaObject)
          mockTransactionStatus(publicationInfo.transaction.transactionId, TransactionStatus.Pending)
      }

      publishOpsForBatching(ops)
      objectManagementService.submitReceivedObjects().value.futureValue.toOption.nonEmpty must be(true)

      verify(ledger, times(2))
        .publish(*) // publish only merged objects

      assert(estimateTxMetadataSize(atalaObjectsMerged.head) < cardano.TX_METADATA_MAX_SIZE)
      assert(estimateTxMetadataSize(atalaObjectsMerged(1)) < cardano.TX_METADATA_MAX_SIZE)
      assert(
        estimateTxMetadataSize(atalaObjectsMerged(1)) > cardano.TX_METADATA_MAX_SIZE / 2
      ) // check that merged object is big enough

      DataPreparation.getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.Pending).size must be(2)
      val notPublishedObjectIds =
        AtalaObjectsDAO.getNotPublishedObjectIds.transact(database).unsafeToFuture().futureValue
      notPublishedObjectIds.size must be(0) // no pending objects
    }

    "record published operations even if others are failed" in {
      val (_, atalaObjectsMerged, publications, ops) = setUpMultipleOperationsPublishing(numOps = 40)

      // first publishing is failed
      doReturn(Future.successful(Left(CardanoWalletError("UtxoTooSmall", CardanoWalletErrorCode.UtxoTooSmall))))
        .when(ledger)
        .publish(atalaObjectsMerged.head)

      // second is ok
      doReturn(Future.successful(Right(publications.last)))
        .when(ledger)
        .publish(atalaObjectsMerged.last)
      mockTransactionStatus(publications.last.transaction.transactionId, TransactionStatus.Pending)

      publishOpsForBatching(ops)
      objectManagementService.submitReceivedObjects().value.futureValue.toOption.nonEmpty must be(true)

      verify(ledger, times(2))
        .publish(*) // publish only merged objects

      DataPreparation.getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.Pending).size must be(1)
      val notPublishedObjectIds =
        AtalaObjectsDAO.getNotPublishedObjectIds.transact(database).unsafeToFuture().futureValue
      notPublishedObjectIds.size must be(1) // no pending objects

      // after publication second transaction becomes InLedger
      mockTransactionStatus(publications.last.transaction.transactionId, TransactionStatus.InLedger)

      // publishing the first operation while retrying becomes ok
      doReturn(Future.successful(Right(publications.dropRight(1).last)))
        .when(ledger)
        .publish(atalaObjectsMerged.head)
      mockTransactionStatus(publications.dropRight(1).last.transaction.transactionId, TransactionStatus.Pending)

      // updates statuses for inLedger submissions
      // note that we're not resubmitting the first object here since it wasn't published at all
      objectManagementService.retryOldPendingTransactions().futureValue
      DataPreparation.getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.Pending).size must be(0)

      // resubmits object1
      objectManagementService.submitReceivedObjects().value.futureValue.toOption.nonEmpty must be(true)
      val notPublishedObjectIds2 =
        AtalaObjectsDAO.getNotPublishedObjectIds.transact(database).unsafeToFuture().futureValue
      notPublishedObjectIds2.size must be(0) // no pending objects
    }

    "not delete already published transaction" in {
      val (atalaObjects, atalaObjectsMerged, publications, ops) =
        setUpMultipleOperationsPublishing(numOps = 3, numPubsAdditional = 1)
      val opInLedger = BlockProcessingServiceSpec.signOperation(
        DataPreparation.exampleOperation,
        s"master${ops.size}",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val objInLedger = createAtalaObject(block = createBlock(opInLedger))

      (atalaObjects ++ atalaObjectsMerged :+ objInLedger).zip(publications).foreach {
        case (atalaObject, publicationInfo) =>
          doReturn(Future.successful(Right(publicationInfo))).when(ledger).publish(atalaObject)
          mockTransactionStatus(publicationInfo.transaction.transactionId, TransactionStatus.Pending)
      }
      publications.take(atalaObjects.size).foreach { publicationInfo =>
        doReturn(Future.successful(Right(()))).when(ledger).deleteTransaction(publicationInfo.transaction.transactionId)
      }
      val inLedgerTransactionId =
        publications.drop(atalaObjects.size + atalaObjectsMerged.size).head.transaction.transactionId
      doReturn(
        Future.successful(Left(CardanoWalletError("Too late", CardanoWalletErrorCode.TransactionAlreadyInLedger)))
      ).when(ledger).deleteTransaction(inLedgerTransactionId)

      publishOpsSequentially(ops :+ opInLedger)

      DataPreparation.getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.InLedger).size must be(0)
      objectManagementService.retryOldPendingTransactions().futureValue must be(1)

      val inLedgerTxs = DataPreparation.getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.InLedger)
      inLedgerTxs.size must be(1)
      inLedgerTxs.head.transactionId must be(inLedgerTransactionId)
    }
  }

  // needed because mockito doesn't interact too nicely with value classes
  private def anyTransactionIdMatcher = mockito.ArgumentMatchers.any[Array[Byte]].asInstanceOf[TransactionId]

  "ObjectManagementService.saveObject" should {
    "add object to the database when nonexistent (unpublished)" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)
      val obj = createAtalaObject()

      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.transaction.value mustBe dummyTransactionInfo
      // TODO: Once processing is async, test the object is not processed
    }

    "update object to the database when existing without transaction info (published but not confirmed)" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)
      val signedOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val obj = createAtalaObject(createBlock(signedOperation))
      publishSingleOperationAndFlush(signedOperation)

      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.transaction.value mustBe dummyTransactionInfo
      // TODO: Once processing is async, test the object is not processed
    }

    "not update the object when existing with transaction info (confirmed)" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)
      val obj = createAtalaObject()

      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue
      val dummyTransactionInfo2 = TransactionInfo(
        transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).getValue).value,
        ledger = Ledger.InMemory,
        block = Some(BlockInfo(number = 100, timestamp = Instant.now, index = 100))
      )
      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo2)).futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.transaction.value mustBe dummyTransactionInfo
      // TODO: Once processing is async, test the object is not processed
    }

    "process the block" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)

      val block = createBlock()
      val obj = createAtalaObject(block)
      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val blockCaptor = ArgCaptor[node_internal.AtalaBlock]
      verify(blockProcessing).processBlock(
        blockCaptor,
        // mockito hates value classes, so we cannot test equality to this argument
        anyTransactionIdMatcher,
        mockito.ArgumentMatchers.eq(dummyTransactionInfo.ledger),
        mockito.ArgumentMatchers.eq(dummyTimestamp),
        mockito.ArgumentMatchers.eq(dummyABSequenceNumber)
      )
      blockCaptor.value mustEqual block

      verifyNoMoreInteractions(blockProcessing)

      val atalaObject = queryAtalaObject(obj)
      atalaObject.processed mustBe true
    }

    def queryAtalaObject(obj: node_internal.AtalaObject): AtalaObjectInfo = {
      AtalaObjectsDAO.get(AtalaObjectId.of(obj)).transact(database).unsafeRunSync().value
    }
  }

  "retryOldPendingTransactions" should {
    val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
    val atalaObject = createAtalaObject(block = createBlock(atalaOperation))

    "ignore in-ledger transactions" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)
      // Publish once and update status
      publishSingleOperationAndFlush(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(
        dummyPublicationInfo.transaction,
        AtalaObjectTransactionSubmissionStatus.InLedger
      )

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore deleted transactions" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(
        dummyPublicationInfo.transaction,
        AtalaObjectTransactionSubmissionStatus.Deleted
      )

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore other ledger's transactions" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      // Simulate the service is restarted with a new ledger type
      doReturn(Ledger.CardanoTestnet).when(ledger).getType

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "retry old pending transactions" in {
      val dummyTransactionId2 = TransactionId.from(SHA256Digest.compute("id2".getBytes).getValue).value
      val dummyTransactionInfo2 = dummyTransactionInfo.copy(transactionId = dummyTransactionId2)
      val dummyPublicationInfo2 = dummyPublicationInfo.copy(transaction = dummyTransactionInfo2)
      // Return dummyTransactionInfo and then dummyTransactionInfo2
      doReturn(Future.successful(Right(dummyPublicationInfo)), Future.successful(Right(dummyPublicationInfo2)))
        .when(ledger)
        .publish(*)
      doReturn(Future.successful(Right(()))).when(ledger).deleteTransaction(dummyTransactionInfo.transactionId)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      mockTransactionStatus(dummyTransactionInfo.transactionId, TransactionStatus.Pending)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published twice and deleted the first one
      verify(ledger, times(2)).publish(atalaObject)
      verify(ledger).deleteTransaction(dummyTransactionInfo.transactionId)
    }

    "merge several operations in one transaction while retrying" in {
      val (atalaObjects, atalaObjectsMerged, publications, ops) = setUpMultipleOperationsPublishing(numOps = 40)

      (atalaObjects ++ atalaObjectsMerged).zip(publications).foreach {
        case (atalaObject, publicationInfo) =>
          doReturn(Future.successful(Right(publicationInfo))).when(ledger).publish(atalaObject)
          mockTransactionStatus(publicationInfo.transaction.transactionId, TransactionStatus.Pending)
      }
      publications.dropRight(atalaObjectsMerged.size).foreach { publicationInfo =>
        doReturn(Future.successful(Right(()))).when(ledger).deleteTransaction(publicationInfo.transaction.transactionId)
      }

      // publish operations sequentially because we want to preserve the order by timestamps
      publishOpsSequentially(ops)

      objectManagementService.retryOldPendingTransactions().futureValue

      verify(ledger, times(atalaObjects.size + 2))
        .publish(*) // publish transactions for initial objects and for two new objects

      assert(estimateTxMetadataSize(atalaObjectsMerged.head) < cardano.TX_METADATA_MAX_SIZE)
      assert(estimateTxMetadataSize(atalaObjectsMerged(1)) < cardano.TX_METADATA_MAX_SIZE)
      assert(
        estimateTxMetadataSize(atalaObjectsMerged(1)) > cardano.TX_METADATA_MAX_SIZE / 2
      ) // check that merged object is big enough

      DataPreparation.getSubmissionsByStatus(AtalaObjectTransactionSubmissionStatus.Pending).size must be(2)

      val notPublishedObjectIds =
        AtalaObjectsDAO.getNotPublishedObjectIds.transact(database).unsafeToFuture().futureValue
      notPublishedObjectIds.size must be(0) // no pending objects
    }

    "not retry new pending transactions" in {
      // Use a service that does have a 10-minute timeout for pending transactions
      val objectManagementService =
        ObjectManagementService(
          ObjectManagementService.Config(ledgerPendingTransactionTimeout = Duration.ofMinutes(10)),
          ledger,
          blockProcessing
        )
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      mockTransactionStatus(dummyTransactionInfo.transactionId, TransactionStatus.Pending)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(dummyTransactionInfo.transactionId)
    }

    "not retry in-ledger transactions" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)
      publishSingleOperationAndFlush(atalaOperation).futureValue
      mockTransactionStatus(dummyTransactionInfo.transactionId, TransactionStatus.InLedger)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(dummyTransactionInfo.transactionId)
    }
  }

  def mockTransactionStatus(transactionId: TransactionId, status: TransactionStatus): Unit = {
    doReturn(Future.successful(Right(TransactionDetails(transactionId, status))))
      .when(ledger)
      .getTransactionDetails(transactionId)
    ()
  }

  private def publishOpsForBatching(ops: List[SignedAtalaOperation]): Unit =
    ops.zipWithIndex.foreach {
      case (atalaOperation, index) =>
        withClue(s"publishing operation #$index") {
          objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
        }
    }

  private def publishOpsSequentially(ops: List[SignedAtalaOperation]): Unit =
    ops.zipWithIndex.foreach {
      case (atalaOperation, index) =>
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
    val atalaObjectsMerged = collection.mutable.ArrayBuffer.empty[node_internal.AtalaObject]
    atalaOperations.reverse.foreach { op =>
      val nextAccOps = op +: accOps
      val curObj = createAtalaObject(
        block = node_internal.AtalaBlock(version = "1.0", operations = nextAccOps),
        opsCount = nextAccOps.size
      )

      if (estimateTxMetadataSize(curObj) >= cardano.TX_METADATA_MAX_SIZE) {
        assert(oldObj != null)
        atalaObjectsMerged.append(oldObj)
        oldObj = createAtalaObject(block = node_internal.AtalaBlock(version = "1.0", operations = List(op)))
        accOps = List(op)
      } else {
        oldObj = curObj
        accOps = nextAccOps
      }
    }
    if (oldObj != null) atalaObjectsMerged.append(oldObj)

    val dummyTransactionIds =
      (0 to (atalaOperations.size + atalaObjectsMerged.size + numPubsAdditional))
        .map { index =>
          TransactionId.from(SHA256Digest.compute(s"id$index".getBytes).getValue).value
        }
    val dummyTransactionInfos = dummyTransactionIds.map { transactionId =>
      dummyTransactionInfo.copy(transactionId = transactionId)
    }
    val dummyPublicationInfos = dummyTransactionInfos.map { transactionInfo =>
      dummyPublicationInfo.copy(transaction = transactionInfo)
    }
    (atalaObjects, atalaObjectsMerged.reverse.toList, dummyPublicationInfos.toList, atalaOperations)
  }

  private def publishSingleOperationAndFlush(signedAtalaOperation: SignedAtalaOperation): Future[AtalaOperationId] = {
    for {
      atalaOperationId <- objectManagementService.publishSingleAtalaOperation(signedAtalaOperation)
      _ <- objectManagementService.submitReceivedObjects().value
    } yield atalaOperationId
  }

  private def publishOperationsAndFlush(ops: SignedAtalaOperation*): Future[List[AtalaOperationId]] = {
    for {
      ids <- objectManagementService.publishAtalaOperations(ops: _*)
      _ <- objectManagementService.submitReceivedObjects().value
    } yield ids
  }

  private def queryTransactionSubmissions(
      status: AtalaObjectTransactionSubmissionStatus
  ): List[AtalaObjectTransactionSubmission] = {
    // Query into the future to return all of them
    AtalaObjectTransactionSubmissionsDAO
      .getBy(Instant.now.plus(Duration.ofSeconds(1)), status, ledger.getType)
      .transact(database)
      .unsafeRunSync()
  }

  protected def createBlock(
      signedOperation: node_models.SignedAtalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
  ): node_internal.AtalaBlock = {
    node_internal.AtalaBlock(version = "1.0", operations = Seq(signedOperation))
  }

  protected def createAtalaObject(
      block: node_internal.AtalaBlock = createBlock(),
      opsCount: Int = 1
  ): node_internal.AtalaObject =
    node_internal
      .AtalaObject(
        blockOperationCount = opsCount
      )
      .withBlockContent(block)

  def setAtalaObjectTransactionSubmissionStatus(
      transaction: TransactionInfo,
      status: AtalaObjectTransactionSubmissionStatus
  ): Unit = {
    AtalaObjectTransactionSubmissionsDAO
      .updateStatus(transaction.ledger, transaction.transactionId, status)
      .transact(database)
      .unsafeRunSync()
    ()
  }
}

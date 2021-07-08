package io.iohk.atala.prism.node.services

import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.{EC, ECKeyPair, SHA256Digest}
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
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectInfo,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus,
  AtalaOperationStatus
}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectTransactionSubmissionsDAO, AtalaObjectsDAO}
import io.iohk.atala.prism.node.services.ObjectManagementService.{
  AtalaObjectTransactionInfo,
  AtalaObjectTransactionStatus
}
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.{DataPreparation, PublicationInfo, UnderlyingLedger, objects}
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
            CreateDIDOperationSpec.protoECKeyFromPublicKey(keyPair.publicKey)
          )
        )
      })
  }

  val exampleSignedOperations: Seq[node_models.SignedAtalaOperation] = exampleOperations.map { operation =>
    BlockProcessingServiceSpec.signOperation(operation, "master", CreateDIDOperationSpec.masterKeys.privateKey)
  }
}

class ObjectManagementServiceSpec
    extends AtalaWithPostgresSpec
    with MockitoSugar
    with ResetMocksAfterEachTest
    with BeforeAndAfterEach {
  import ObjectManagementServiceSpec._

  private val storage = new objects.ObjectStorageService.InMemory

  private val ledger: UnderlyingLedger = mock[UnderlyingLedger]
  private val blockProcessing: BlockProcessingService = mock[BlockProcessingService]

  private lazy val objectManagementService =
    ObjectManagementService(
      ObjectManagementService.Config(ledgerPendingTransactionTimeout = Duration.ZERO),
      storage,
      ledger,
      blockProcessing
    )

  private val dummyTime = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)

  private val dummyBlockNo = 17
  private val dummyTimestamp = dummyTime.atalaBlockTimestamp
  private val dummyABSequenceNumber = dummyTime.atalaBlockSequenceNumber
  private val dummyTransactionInfo =
    TransactionInfo(
      transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
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
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId = BlockProcessingServiceSpec.signedCreateDidOperationId
      val returnedOperationId = objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      returnedOperationId mustBe atalaOperationId

      val atalaOperationInfo = objectManagementService.getOperationInfo(atalaOperationId).futureValue.value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(Some(AtalaObjectTransactionSubmissionStatus.Pending))
      atalaOperationInfo.transactionId.value must be(dummyPublicationInfo.transaction.transactionId)
    }

    "ignore publishing duplicate operation" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId = BlockProcessingServiceSpec.signedCreateDidOperationId
      val returnedAtalaOperation = objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      returnedAtalaOperation mustBe atalaOperationId

      ScalaFutures.whenReady(objectManagementService.publishSingleAtalaOperation(atalaOperation).failed) { err =>
        err mustBe a[DuplicateAtalaBlock]
      }

      val atalaOperationInfo = objectManagementService.getOperationInfo(atalaOperationId).futureValue.value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(Some(AtalaObjectTransactionSubmissionStatus.Pending))
      atalaOperationInfo.transactionId.value must be(dummyPublicationInfo.transaction.transactionId)
    }

    "ignore publishing duplicate operation in the same block" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId = BlockProcessingServiceSpec.signedCreateDidOperationId

      val opIds = objectManagementService.publishAtalaOperations(atalaOperation, atalaOperation).futureValue

      opIds.size mustBe 2
      opIds.head mustBe atalaOperationId
      opIds.last mustBe atalaOperationId

      val atalaOperationInfo = objectManagementService.getOperationInfo(atalaOperationId).futureValue.value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(Some(AtalaObjectTransactionSubmissionStatus.Pending))
    }

    "put block content onto the ledger when supported" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      val returnedOperationId =
        objectManagementService
          .publishSingleAtalaOperation(BlockProcessingServiceSpec.signedCreateDidOperation)
          .futureValue

      returnedOperationId mustBe BlockProcessingServiceSpec.signedCreateDidOperationId
      // Verify published AtalaObject
      val atalaObjectCaptor = ArgCaptor[node_internal.AtalaObject]
      verify(ledger).publish(atalaObjectCaptor)
      val atalaObject = atalaObjectCaptor.value
      val atalaBlock = atalaObject.block.blockContent.value
      atalaBlock.operations must contain theSameElementsAs Seq(BlockProcessingServiceSpec.signedCreateDidOperation)
      // Verify off-chain storage is not used
      verifyStorage(atalaBlock.toByteArray, expectedInStorage = false)
      // Verify transaction submission
      val transactionSubmissions = queryTransactionSubmissions(AtalaObjectTransactionSubmissionStatus.Pending)
      transactionSubmissions.size mustBe 1

      val operationInfo = objectManagementService.getOperationInfo(returnedOperationId).futureValue.value
      operationInfo.operationId must be(returnedOperationId)
      operationInfo.transactionSubmissionStatus.value must be(AtalaObjectTransactionSubmissionStatus.Pending)
      operationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      operationInfo.transactionId.value must be(dummyPublicationInfo.transaction.transactionId)
    }

    "put reference to block onto the ledger when on-chain data not supported" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(false).when(ledger).supportsOnChainData

      val returnedOperationId =
        objectManagementService
          .publishSingleAtalaOperation(BlockProcessingServiceSpec.signedCreateDidOperation)
          .futureValue

      returnedOperationId mustBe BlockProcessingServiceSpec.signedCreateDidOperationId
      // Verify published AtalaObject
      val atalaObjectCaptor = ArgCaptor[node_internal.AtalaObject]
      verify(ledger).publish(atalaObjectCaptor)
      val atalaObject = atalaObjectCaptor.value
      val atalaBlock = getBlockFromStorage(atalaObject)
      atalaBlock.operations must contain theSameElementsAs Seq(BlockProcessingServiceSpec.signedCreateDidOperation)
      // Verify off-chain storage is used
      verifyStorage(atalaBlock.toByteArray, expectedInStorage = true)
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
      doReturn(Future.successful(inLedgerPublication)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      val returnedOperationId =
        objectManagementService
          .publishSingleAtalaOperation(BlockProcessingServiceSpec.signedCreateDidOperation)
          .futureValue

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

    def verifyStorage(bytes: Array[Byte], expectedInStorage: Boolean): Unit = {
      val maybeBytes = storage.get(SHA256Digest.compute(bytes).hexValue).futureValue
      if (expectedInStorage) {
        maybeBytes.value mustBe bytes
      } else {
        maybeBytes mustBe None
      }
      ()
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
      objectManagementService.publishSingleAtalaOperation(signedOperation)

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
        transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
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

    "add objects by content and by reference" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)

      val blocks = for ((signedOp, i) <- exampleSignedOperations.zipWithIndex) yield {
        val includeBlock = (i & 1) == 1

        val block = createBlock(signedOp)
        val obj = createAtalaObject(block, includeBlock)

        objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

        val atalaObject = queryAtalaObject(obj)
        atalaObject.transaction.value mustBe dummyTransactionInfo
        atalaObject.processed mustBe true
        atalaObject.byteContent mustBe obj.toByteArray

        block
      }

      val blockCaptor = ArgCaptor[node_internal.AtalaBlock]
      verify(blockProcessing, times(blocks.size))
        .processBlock(
          blockCaptor,
          anyTransactionIdMatcher,
          mockito.ArgumentMatchers.any(),
          mockito.ArgumentMatchers.any(),
          mockito.ArgumentMatchers.any()
        )
      blockCaptor.values must contain theSameElementsAs blocks

      verifyNoMoreInteractions(blockProcessing)
    }

    def queryAtalaObject(obj: node_internal.AtalaObject): AtalaObjectInfo = {
      AtalaObjectsDAO.get(AtalaObjectId.of(obj)).transact(database).unsafeRunSync().value
    }
  }

  "retryOldPendingTransactions" should {
    val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
    val atalaObject = createAtalaObject(block = createBlock(atalaOperation))

    def mockTransactionStatus(transactionId: TransactionId, status: TransactionStatus): Unit = {
      doReturn(Future.successful(TransactionDetails(transactionId, status)))
        .when(ledger)
        .getTransactionDetails(transactionId)
      ()
    }

    "ignore in-ledger transactions" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      // Publish once and update status
      objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(
        dummyPublicationInfo.transaction,
        AtalaObjectTransactionSubmissionStatus.InLedger
      )

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore deleted transactions" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(
        dummyPublicationInfo.transaction,
        AtalaObjectTransactionSubmissionStatus.Deleted
      )

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore other ledger's transactions" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      // Simulate the service is restarted with a new ledger type
      doReturn(Ledger.CardanoTestnet).when(ledger).getType

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "retry old pending transactions" in {
      val dummyTransactionId2 = TransactionId.from(SHA256Digest.compute("id2".getBytes).value).value
      val dummyTransactionInfo2 = dummyTransactionInfo.copy(transactionId = dummyTransactionId2)
      val dummyPublicationInfo2 = dummyPublicationInfo.copy(transaction = dummyTransactionInfo2)
      // Return dummyTransactionInfo and then dummyTransactionInfo2
      doReturn(Future.successful(dummyPublicationInfo), Future.successful(dummyPublicationInfo2))
        .when(ledger)
        .publish(*)
      doReturn(Future.unit).when(ledger).deleteTransaction(dummyTransactionInfo.transactionId)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      mockTransactionStatus(dummyTransactionInfo.transactionId, TransactionStatus.Pending)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published twice and deleted the first one
      verify(ledger, times(2)).publish(atalaObject)
      verify(ledger).deleteTransaction(dummyTransactionInfo.transactionId)
    }

    "merge several operations in one transaction while retrying" in {
      val atalaOperations = (0 to 20).toList.map { masterId =>
        BlockProcessingServiceSpec.signOperation(
          BlockProcessingServiceSpec.createDidOperation,
          s"master$masterId",
          CreateDIDOperationSpec.masterKeys.privateKey
        )
      }
      val atalaObjects = atalaOperations.map { op =>
        createAtalaObject(block = createBlock(op))
      }
      val objs = atalaObjects.map { obj =>
        AtalaObjectInfo(AtalaObjectId.of(obj), obj.toByteArray, processed = false)
      }.reverse
      var accObj = objs.head

      val secondMergedBlockSize = 1 + objs.tail.takeWhile { obj =>
        obj.mergeIfPossible(accObj) match {
          case Some(mergedObj) =>
            accObj = mergedObj
            true
          case None =>
            false
        }
      }.size
      val firstMergedBlockSize = atalaObjects.size - secondMergedBlockSize

      val atalaObjectMerged1 = createAtalaObject(
        block = node_internal.AtalaBlock(version = "1.0", operations = atalaOperations.take(firstMergedBlockSize)),
        opsCount = firstMergedBlockSize
      )
      val atalaObjectMerged2 = createAtalaObject(
        block = node_internal.AtalaBlock(version = "1.0", operations = atalaOperations.drop(firstMergedBlockSize)),
        opsCount = secondMergedBlockSize
      )

      val dummyTransactionIds = (0 to (atalaOperations.size + 2)).map { index =>
        TransactionId.from(SHA256Digest.compute(s"id$index".getBytes).value).value
      }
      val dummyTransactionInfos = dummyTransactionIds.map { transactionId =>
        dummyTransactionInfo.copy(transactionId = transactionId)
      }
      val dummyPublicationInfos = dummyTransactionInfos.map { transactionInfo =>
        dummyPublicationInfo.copy(transaction = transactionInfo)
      }

      doReturn(true).when(ledger).supportsOnChainData
      (atalaObjects :+ atalaObjectMerged1 :+ atalaObjectMerged2).zip(dummyPublicationInfos).foreach {
        case (atalaObject, publicationInfo) =>
          doReturn(Future.successful(publicationInfo)).when(ledger).publish(atalaObject)
          mockTransactionStatus(publicationInfo.transaction.transactionId, TransactionStatus.Pending)
      }
      dummyPublicationInfos.dropRight(2).foreach { publicationInfo =>
        doReturn(Future.unit).when(ledger).deleteTransaction(publicationInfo.transaction.transactionId)
      }

      // publish operations sequentially because we want to preserve the order by timestamps
      atalaOperations.foreach { atalaOperation =>
        objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      }

      objectManagementService.retryOldPendingTransactions().futureValue

      verify(ledger, times(atalaObjects.size + 2))
        .publish(*) // publish transactions for initial objects and for two new objects

      assert(estimateTxMetadataSize(atalaObjectMerged1) < cardano.TX_METADATA_MAX_SIZE)
      assert(estimateTxMetadataSize(atalaObjectMerged2) < cardano.TX_METADATA_MAX_SIZE)
      assert(
        estimateTxMetadataSize(atalaObjectMerged2) > cardano.TX_METADATA_MAX_SIZE / 2
      ) // check that merged object is big enough

      DataPreparation.getPendingSubmissions().size must be(2)
    }

    "not retry new pending transactions" in {
      // Use a service that does have a 10-minute timeout for pending transactions
      val objectManagementService =
        ObjectManagementService(
          ObjectManagementService.Config(ledgerPendingTransactionTimeout = Duration.ofMinutes(10)),
          storage,
          ledger,
          blockProcessing
        )
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      mockTransactionStatus(dummyTransactionInfo.transactionId, TransactionStatus.Pending)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(dummyTransactionInfo.transactionId)
    }

    "not retry in-ledger transactions" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      mockTransactionStatus(dummyTransactionInfo.transactionId, TransactionStatus.InLedger)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(dummyTransactionInfo.transactionId)
    }
  }

  "ObjectManagementService.getLatestTransactionAndStatus" should {
    val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
    val atalaObject = createAtalaObject(block = createBlock(atalaOperation))
    val transactionInfo = TransactionInfo(
      transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
      ledger = Ledger.InMemory
    )
    val transactionInfoWithBlock = transactionInfo.copy(block =
      Some(BlockInfo(number = 1, timestamp = dummyTimestamp, index = dummyABSequenceNumber))
    )
    val publicationInfo = PublicationInfo(transactionInfo, TransactionStatus.Pending)

    "return None when not found" in {
      DataPreparation.updateLastSyncedBlock(dummyBlockNo, dummyTimestamp)

      val status = objectManagementService.getLatestTransactionAndStatus(transactionInfo).futureValue

      status mustBe None
    }

    "return Pending when pending" in {
      doReturn(Future.successful(publicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      DataPreparation.updateLastSyncedBlock(dummyBlockNo, dummyTimestamp)

      objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue

      val status = objectManagementService
        .getLatestTransactionAndStatus(transactionInfo)
        .futureValue
        .value

      status mustBe AtalaObjectTransactionInfo(transactionInfo, AtalaObjectTransactionStatus.Pending)
    }

    "return Pending when deleted" in {
      doReturn(Future.successful(publicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(
        publicationInfo.transaction,
        AtalaObjectTransactionSubmissionStatus.Deleted
      )

      val status = objectManagementService
        .getLatestTransactionAndStatus(transactionInfo)
        .futureValue
        .value

      status mustBe AtalaObjectTransactionInfo(transactionInfo, AtalaObjectTransactionStatus.Pending)
    }

    "return InLedger when in ledger" in {
      doReturn(Future.successful(publicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(
        publicationInfo.transaction,
        AtalaObjectTransactionSubmissionStatus.InLedger
      )

      val status = objectManagementService
        .getLatestTransactionAndStatus(transactionInfo)
        .futureValue
        .value

      status mustBe AtalaObjectTransactionInfo(transactionInfo, AtalaObjectTransactionStatus.InLedger)
    }

    "return Confirmed when object has been processed" in {
      doReturn(Future.successful(publicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)

      DataPreparation.updateLastSyncedBlock(dummyBlockNo, dummyTimestamp)

      objectManagementService.publishSingleAtalaOperation(atalaOperation).futureValue
      // Need block info when saving an object as it comes from the ledger
      objectManagementService.saveObject(AtalaObjectNotification(atalaObject, transactionInfoWithBlock)).futureValue

      val status = objectManagementService
        .getLatestTransactionAndStatus(transactionInfo)
        .futureValue
        .value

      status mustBe AtalaObjectTransactionInfo(transactionInfoWithBlock, AtalaObjectTransactionStatus.Confirmed)
    }
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

  protected def getBlockFromStorage(atalaObject: node_internal.AtalaObject): node_internal.AtalaBlock = {
    val atalaBlockHash = SHA256Digest.fromVectorUnsafe(atalaObject.getBlockHash.toByteArray.toVector)
    val atalaBlockData = storage.get(atalaBlockHash.hexValue).futureValue.value
    node_internal.AtalaBlock.parseFrom(atalaBlockData)
  }

  protected def createBlock(
      signedOperation: node_models.SignedAtalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
  ): node_internal.AtalaBlock = {
    node_internal.AtalaBlock(version = "1.0", operations = Seq(signedOperation))
  }

  protected def storeBlock(block: node_internal.AtalaBlock): SHA256Digest = {
    val blockBytes = block.toByteArray
    val blockHash = SHA256Digest.compute(blockBytes)
    storage.put(blockHash.hexValue, blockBytes)
    blockHash
  }

  protected def createAtalaObject(
      block: node_internal.AtalaBlock = createBlock(),
      includeBlock: Boolean = true,
      opsCount: Int = 1
  ): node_internal.AtalaObject = {
    if (includeBlock) {
      node_internal.AtalaObject(
        block = node_internal.AtalaObject.Block.BlockContent(block),
        blockOperationCount = opsCount
      )
    } else {
      val blockHash = storeBlock(block)
      node_internal.AtalaObject(
        block = node_internal.AtalaObject.Block.BlockHash(ByteString.copyFrom(blockHash.value.toArray)),
        blockOperationCount = opsCount
      )
    }
  }

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

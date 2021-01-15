package io.iohk.atala.prism.node.services

import java.time.{Duration, Instant}

import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.implicits._
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
import io.iohk.atala.prism.node.models.{
  AtalaObject,
  AtalaObjectId,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus
}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectTransactionSubmissionsDAO, AtalaObjectsDAO}
import io.iohk.atala.prism.node.services.ObjectManagementService.{
  AtalaObjectTransactionInfo,
  AtalaObjectTransactionStatus
}
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.{AtalaLedger, PublicationInfo, objects}
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.AtalaWithPostgresSpec
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.mockito
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.concurrent.duration._

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

class ObjectManagementServiceSpec extends AtalaWithPostgresSpec with MockitoSugar with BeforeAndAfterEach {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

  import ObjectManagementServiceSpec._

  private val storage = new objects.ObjectStorageService.InMemory

  private val ledger: AtalaLedger = mock[AtalaLedger]
  private val blockProcessing: BlockProcessingService = mock[BlockProcessingService]

  private lazy val objectManagementService =
    ObjectManagementService(
      ObjectManagementService.Config(ledgerPendingTransactionTimeout = Duration.ZERO),
      storage,
      ledger,
      blockProcessing
    )

  private val dummyTime = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)

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

    reset(ledger)
    reset(blockProcessing)

    doReturn(Ledger.InMemory).when(ledger).getType
    ()
  }

  "ObjectManagementService.publishAtalaOperation" should {
    "put block content onto the ledger when supported" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      val transactionInfo =
        objectManagementService.publishAtalaOperation(BlockProcessingServiceSpec.signedCreateDidOperation).futureValue

      transactionInfo mustBe dummyTransactionInfo
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
      val transactionSubmission = transactionSubmissions.head
      transactionSubmission.ledger mustBe transactionInfo.ledger
      transactionSubmission.transactionId mustBe transactionInfo.transactionId
    }

    "put reference to block onto the ledger when on-chain data not supported" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(false).when(ledger).supportsOnChainData

      val transactionInfo =
        objectManagementService.publishAtalaOperation(BlockProcessingServiceSpec.signedCreateDidOperation).futureValue

      transactionInfo mustBe dummyTransactionInfo
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
      val transactionSubmission = transactionSubmissions.head
      transactionSubmission.ledger mustBe transactionInfo.ledger
      transactionSubmission.transactionId mustBe transactionInfo.transactionId
    }

    "record immediate in-ledger transactions" in {
      val inLedgerPublication = dummyPublicationInfo.copy(status = TransactionStatus.InLedger)
      doReturn(Future.successful(inLedgerPublication)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      val transactionInfo =
        objectManagementService.publishAtalaOperation(BlockProcessingServiceSpec.signedCreateDidOperation).futureValue

      // Verify transaction submission
      val transactionSubmissions = queryTransactionSubmissions(AtalaObjectTransactionSubmissionStatus.InLedger)
      transactionSubmissions.size mustBe 1
      val transactionSubmission = transactionSubmissions.head
      transactionSubmission.ledger mustBe transactionInfo.ledger
      transactionSubmission.transactionId mustBe transactionInfo.transactionId
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
      objectManagementService.publishAtalaOperation(signedOperation)

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

    def queryAtalaObject(obj: node_internal.AtalaObject): AtalaObject = {
      AtalaObjectsDAO.get(AtalaObjectId.of(obj)).transact(database).unsafeRunSync().value
    }
  }

  "retryOldPendingTransactions" should {
    val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
    val atalaObject = createAtalaObject(block = createBlock(atalaOperation))
    val atalaObjectId = AtalaObjectId.of(atalaObject)

    def mockTransactionStatus(transactionId: TransactionId, status: TransactionStatus): Unit = {
      doReturn(Future.successful(TransactionDetails(transactionId, status)))
        .when(ledger)
        .getTransactionDetails(transactionId)
      ()
    }

    def setAtalaObjectTransactionSubmissionStatus(
        atalaObjectId: AtalaObjectId,
        status: AtalaObjectTransactionSubmissionStatus
    ): Unit = {
      AtalaObjectTransactionSubmissionsDAO
        .updateStatus(atalaObjectId, status)
        .transact(database)
        .unsafeRunSync()
      ()
    }

    "ignore in-ledger transactions" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      // Publish once and update status
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(atalaObjectId, AtalaObjectTransactionSubmissionStatus.InLedger)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore deleted transactions" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(atalaObjectId, AtalaObjectTransactionSubmissionStatus.Deleted)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore other ledger's transactions" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
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
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      mockTransactionStatus(dummyTransactionInfo.transactionId, TransactionStatus.Pending)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published twice and deleted the first one
      verify(ledger, times(2)).publish(atalaObject)
      verify(ledger).deleteTransaction(dummyTransactionInfo.transactionId)
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
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      mockTransactionStatus(dummyTransactionInfo.transactionId, TransactionStatus.Pending)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(dummyTransactionInfo.transactionId)
    }

    "not retry in-ledger transactions" in {
      doReturn(Future.successful(dummyPublicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
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
    val atalaObjectId = AtalaObjectId.of(atalaObject)
    val transactionInfo = TransactionInfo(
      transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
      ledger = Ledger.InMemory
    )
    val transactionInfoWithBlock = transactionInfo.copy(block =
      Some(BlockInfo(number = 1, timestamp = dummyTimestamp, index = dummyABSequenceNumber))
    )
    val publicationInfo = PublicationInfo(transactionInfo, TransactionStatus.Pending)

    def setAtalaObjectTransactionSubmissionStatus(
        atalaObjectId: AtalaObjectId,
        status: AtalaObjectTransactionSubmissionStatus
    ): Unit = {
      AtalaObjectTransactionSubmissionsDAO
        .updateStatus(atalaObjectId, status)
        .transact(database)
        .unsafeRunSync()
      ()
    }

    "return None when not found" in {
      val status = objectManagementService.getLatestTransactionAndStatus(transactionInfo).futureValue

      status mustBe None
    }

    "return Pending when pending" in {
      doReturn(Future.successful(publicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue

      val status = objectManagementService.getLatestTransactionAndStatus(transactionInfo).futureValue.value

      status mustBe AtalaObjectTransactionInfo(transactionInfo, AtalaObjectTransactionStatus.Pending)
    }

    "return Pending when deleted" in {
      doReturn(Future.successful(publicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(atalaObjectId, AtalaObjectTransactionSubmissionStatus.Deleted)

      val status = objectManagementService.getLatestTransactionAndStatus(transactionInfo).futureValue.value

      status mustBe AtalaObjectTransactionInfo(transactionInfo, AtalaObjectTransactionStatus.Pending)
    }

    "return InLedger when in ledger" in {
      doReturn(Future.successful(publicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(atalaObjectId, AtalaObjectTransactionSubmissionStatus.InLedger)

      val status = objectManagementService.getLatestTransactionAndStatus(transactionInfo).futureValue.value

      status mustBe AtalaObjectTransactionInfo(transactionInfo, AtalaObjectTransactionStatus.InLedger)
    }

    "return Confirmed when object has been processed" in {
      doReturn(Future.successful(publicationInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      // Need block info when saving an object as it comes from the ledger
      objectManagementService.saveObject(AtalaObjectNotification(atalaObject, transactionInfoWithBlock)).futureValue

      val status = objectManagementService.getLatestTransactionAndStatus(transactionInfo).futureValue.value

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
    val atalaBlockHash = SHA256Digest(atalaObject.getBlockHash.toByteArray.toVector)
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
      includeBlock: Boolean = true
  ): node_internal.AtalaObject = {
    if (includeBlock) {
      node_internal.AtalaObject(
        block = node_internal.AtalaObject.Block.BlockContent(block),
        blockOperationCount = 1
      )
    } else {
      val blockHash = storeBlock(block)
      node_internal.AtalaObject(
        block = node_internal.AtalaObject.Block.BlockHash(ByteString.copyFrom(blockHash.value.toArray)),
        blockOperationCount = 1
      )
    }
  }
}

package io.iohk.atala.prism.node.services

import java.time.{Duration, Instant}

import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.implicits._
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
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus
}
import io.iohk.atala.prism.node.operations.{CreateDIDOperationSpec, TimestampInfo}
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectTransactionSubmissionsDAO, AtalaObjectsDAO}
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.{AtalaReferenceLedger, objects}
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
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

class ObjectManagementServiceSpec extends PostgresRepositorySpec with MockitoSugar with BeforeAndAfterEach {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

  import ObjectManagementServiceSpec._

  private val storage = new objects.ObjectStorageService.InMemory

  private val ledger: AtalaReferenceLedger = mock[AtalaReferenceLedger]
  private val blockProcessing: BlockProcessingService = mock[BlockProcessingService]

  private lazy val objectManagementService =
    ObjectManagementService(
      ObjectManagementService.Config(ledgerPendingTransactionTimeout = Duration.ZERO),
      storage,
      ledger,
      blockProcessing
    )

  private val dummyTimestamp = TimestampInfo.dummyTime.atalaBlockTimestamp
  private val dummyABSequenceNumber = TimestampInfo.dummyTime.atalaBlockSequenceNumber
  private val dummyTransactionInfo =
    TransactionInfo(
      transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value,
      ledger = Ledger.InMemory,
      block = Some(BlockInfo(number = 1, timestamp = dummyTimestamp, index = dummyABSequenceNumber))
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(ledger)
    reset(blockProcessing)
  }

  "ObjectManagementService.publishAtalaOperation" should {
    "put block content onto the ledger when supported" in {
      doReturn(Future.successful(dummyTransactionInfo)).when(ledger).publish(*)
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
      verifyStorage(atalaObject.toByteArray, expectedInStorage = false)
      // Verify transaction submission
      val transactionSubmissions = queryPendingTransactionSubmissions()
      transactionSubmissions.size mustBe 1
      val transactionSubmission = transactionSubmissions.head
      transactionSubmission.ledger mustBe transactionInfo.ledger
      transactionSubmission.transactionId mustBe transactionInfo.transactionId
    }

    "put reference to block onto the ledger" in {
      doReturn(Future.successful(dummyTransactionInfo)).when(ledger).publish(*)
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
      verifyStorage(atalaObject.toByteArray, expectedInStorage = true)
      // Verify transaction submission
      val transactionSubmissions = queryPendingTransactionSubmissions()
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

  "ObjectManagementService.saveObject" should {
    "add object to the database when nonexistent (unpublished)" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)
      val obj = createAtalaObject()

      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.transaction.value mustBe dummyTransactionInfo
      // TODO: Once processing is async, test the object is not processed
    }

    "update object to the database when existing without transaction info (published but not confirmed)" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)
      val signedOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val obj = createAtalaObject(createBlock(signedOperation))
      objectManagementService.publishAtalaOperation(signedOperation)

      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.transaction.value mustBe dummyTransactionInfo
      // TODO: Once processing is async, test the object is not processed
    }

    "not update the object when existing with transaction info (confirmed)" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)
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
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)

      val block = createBlock()
      val obj = createAtalaObject(block)
      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val blockCaptor = ArgCaptor[node_internal.AtalaBlock]
      verify(blockProcessing).processBlock(
        blockCaptor,
        mockito.ArgumentMatchers.eq(dummyTimestamp),
        mockito.ArgumentMatchers.eq(dummyABSequenceNumber)
      )
      blockCaptor.value mustEqual block

      verifyNoMoreInteractions(blockProcessing)

      val atalaObject = queryAtalaObject(obj)
      atalaObject.processed mustBe true
    }

    "add objects by content and by reference" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)

      val blocks = for ((signedOp, i) <- exampleSignedOperations.zipWithIndex) yield {
        val includeBlock = (i & 1) == 1

        val block = createBlock(signedOp)
        val obj = createAtalaObject(block, includeBlock)

        objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

        val atalaObject = queryAtalaObject(obj)
        atalaObject.transaction.value mustBe dummyTransactionInfo
        atalaObject.processed mustBe true
        atalaObject.byteContent.isDefined mustBe true

        block
      }

      val blockCaptor = ArgCaptor[node_internal.AtalaBlock]
      verify(blockProcessing, times(blocks.size))
        .processBlock(blockCaptor, mockito.ArgumentMatchers.any(), mockito.ArgumentMatchers.any())
      blockCaptor.values must contain theSameElementsAs blocks

      verifyNoMoreInteractions(blockProcessing)
    }

    def queryAtalaObject(obj: node_internal.AtalaObject): AtalaObject = {
      AtalaObjectsDAO.get(getObjectId(obj)).transact(database).unsafeRunSync().value
    }
  }

  "retryOldPendingTransactions" should {
    val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
    val atalaObject = createAtalaObject(block = createBlock(atalaOperation))
    val atalaObjectId = getObjectId(atalaObject)

    def mockTransactionStatus(transactionId: TransactionId, status: TransactionStatus): Unit = {
      doReturn(Future.successful(TransactionDetails(transactionId, status)))
        .when(ledger)
        .getTransactionDetails(transactionId)
      ()
    }

    def setAtalaObjectTransactionSubmissionStatus(
        atalaObjectId: SHA256Digest,
        status: AtalaObjectTransactionSubmissionStatus
    ): Unit = {
      AtalaObjectTransactionSubmissionsDAO
        .updateStatus(atalaObjectId, status)
        .transact(database)
        .unsafeRunSync()
      ()
    }

    "ignore in-ledger transactions" in {
      doReturn(Future.successful(dummyTransactionInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      // Publish once and update status
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(atalaObjectId, AtalaObjectTransactionSubmissionStatus.InLedger)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "ignore deleted transactions" in {
      doReturn(Future.successful(dummyTransactionInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      setAtalaObjectTransactionSubmissionStatus(atalaObjectId, AtalaObjectTransactionSubmissionStatus.Deleted)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
    }

    "retry old pending transactions" in {
      val dummyTransactionId2 = TransactionId.from(SHA256Digest.compute("id2".getBytes).value).value
      val dummyTransactionInfo2 = dummyTransactionInfo.copy(transactionId = dummyTransactionId2)
      // Return dummyTransactionInfo and then dummyTransactionInfo2
      doReturn(Future.successful(dummyTransactionInfo), Future.successful(dummyTransactionInfo2))
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
      doReturn(Future.successful(dummyTransactionInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      mockTransactionStatus(dummyTransactionInfo.transactionId, TransactionStatus.Pending)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(dummyTransactionInfo.transactionId)
    }

    "not retry in-ledger transactions" in {
      doReturn(Future.successful(dummyTransactionInfo)).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData
      objectManagementService.publishAtalaOperation(atalaOperation).futureValue
      mockTransactionStatus(dummyTransactionInfo.transactionId, TransactionStatus.InLedger)

      objectManagementService.retryOldPendingTransactions().futureValue

      // It should have published only once
      verify(ledger).publish(atalaObject)
      verify(ledger, never).deleteTransaction(dummyTransactionInfo.transactionId)
    }
  }

  private def queryPendingTransactionSubmissions(): List[AtalaObjectTransactionSubmission] = {
    // Query into the future to return all of them
    AtalaObjectTransactionSubmissionsDAO
      .getBy(Instant.now.plus(Duration.ofSeconds(1)), AtalaObjectTransactionSubmissionStatus.Pending)
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

  private def getObjectId(obj: node_internal.AtalaObject): SHA256Digest = {
    SHA256Digest.compute(obj.toByteArray)
  }
}

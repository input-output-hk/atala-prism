package io.iohk.atala.prism.node.services

import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.implicits._
import io.iohk.atala.crypto.{EC, ECKeyPair}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{BlockInfo, Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.models.AtalaObject
import io.iohk.atala.prism.node.operations.{CreateDIDOperationSpec, TimestampInfo}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.{AtalaReferenceLedger, objects}
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.prism.protos.{node_internal, node_models}
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

  private lazy val objectManagementService = new ObjectManagementService(storage, ledger, blockProcessing)

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
      doReturn(Future.successful(())).when(ledger).publish(*)
      doReturn(true).when(ledger).supportsOnChainData

      objectManagementService.publishAtalaOperation(BlockProcessingServiceSpec.signedCreateDidOperation)

      val atalaObjectCaptor = ArgCaptor[node_internal.AtalaObject]
      verify(ledger).publish(atalaObjectCaptor)

      val atalaObject = atalaObjectCaptor.value
      val atalaBlock = atalaObject.block.blockContent.value
      atalaBlock.operations must contain theSameElementsAs Seq(BlockProcessingServiceSpec.signedCreateDidOperation)
    }

    "put reference to block onto the ledger" in {
      doReturn(Future.successful(())).when(ledger).publish(*)
      doReturn(false).when(ledger).supportsOnChainData

      objectManagementService.publishAtalaOperation(BlockProcessingServiceSpec.signedCreateDidOperation)

      val atalaObjectCaptor = ArgCaptor[node_internal.AtalaObject]
      verify(ledger).publish(atalaObjectCaptor)

      val atalaObject = atalaObjectCaptor.value
      val atalaBlock = getBlockFromStorage(atalaObject)
      atalaBlock.operations must contain theSameElementsAs Seq(BlockProcessingServiceSpec.signedCreateDidOperation)
    }

    "put many references onto the ledger" in {
      doReturn(Future.successful(())).when(ledger).publish(*)
      doReturn(false).when(ledger).supportsOnChainData

      Future
        .traverse(exampleSignedOperations) { signedOp =>
          objectManagementService.publishAtalaOperation(signedOp)
        }
        .futureValue

      val atalaObjectCaptor = ArgCaptor[node_internal.AtalaObject]
      verify(ledger, times(exampleOperations.size)).publish(atalaObjectCaptor)

      for ((signedOp, atalaObject) <- exampleSignedOperations zip atalaObjectCaptor.values) {
        val atalaBlock = getBlockFromStorage(atalaObject)
        atalaBlock.operations must contain theSameElementsAs Seq(signedOp)
      }
    }
  }

  "ObjectManagementService.saveObject" should {
    "add object to the database" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)

      val block = exampleBlock()
      val obj = createExampleObject(block)
      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.blockIndex mustBe 1
      // TODO: Once processing is async, test the object is not processed
    }

    "be idempotent - ignore re-adding the same hash" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)

      val block = exampleBlock()
      val obj = createExampleObject(block)
      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.blockIndex mustBe 1
      // TODO: Once processing is async, test the object is not processed
    }

    "process the block" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)

      val block = exampleBlock()
      val obj = createExampleObject(block)
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
      atalaObject.blockIndex mustBe 1
      atalaObject.processed mustBe true
    }

    "add objects by content and by reference" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)

      val blocks = for ((signedOp, i) <- exampleSignedOperations.zipWithIndex) yield {
        val includeBlock = (i & 1) == 1

        val block = exampleBlock(signedOp)
        val obj = createExampleAtalaObject(block, includeBlock)

        objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

        val atalaObject = queryAtalaObject(obj)
        atalaObject.blockIndex mustBe dummyABSequenceNumber
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
      AtalaObjectsDAO.get(SHA256Digest.compute(obj.toByteArray)).transact(database).unsafeRunSync().value
    }
  }

  protected def getBlockFromStorage(atalaObject: node_internal.AtalaObject): node_internal.AtalaBlock = {
    val atalaBlockHash = SHA256Digest(atalaObject.getBlockHash.toByteArray)
    val atalaBlockData = storage.get(atalaBlockHash.hexValue).futureValue.value
    node_internal.AtalaBlock.parseFrom(atalaBlockData)
  }

  protected def exampleBlock(
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

  protected def storeObject(atalaObject: node_internal.AtalaObject): Unit = {
    val objectBytes = atalaObject.toByteArray
    val objectHash = SHA256Digest.compute(objectBytes)
    storage.put(objectHash.hexValue, objectBytes).futureValue
  }

  protected def createExampleObject(block: node_internal.AtalaBlock): node_internal.AtalaObject = {
    val blockBytes = block.toByteArray
    val blockHash = storeBlock(block)

    val atalaObject = node_internal.AtalaObject(
      1,
      blockBytes.length,
      node_internal.AtalaObject.Block.BlockHash(ByteString.copyFrom(blockHash.value))
    )
    storeObject(atalaObject)
    atalaObject
  }

  protected def createExampleAtalaObject(
      block: node_internal.AtalaBlock,
      includeBlock: Boolean
  ): node_internal.AtalaObject = {
    val blockBytes = block.toByteArray

    if (includeBlock) {
      node_internal.AtalaObject(1, blockBytes.length, node_internal.AtalaObject.Block.BlockContent(block))
    } else {
      val blockHash = storeBlock(block)
      node_internal.AtalaObject(
        1,
        blockBytes.length,
        node_internal.AtalaObject.Block.BlockHash(ByteString.copyFrom(blockHash.value))
      )
    }
  }
}

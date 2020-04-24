package io.iohk.node.services

import java.security.KeyPair
import java.time.Instant

import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.implicits._
import io.iohk.cvp.crypto.{ECKeys, SHA256Digest}
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.operations.{CreateDIDOperationSpec, TimestampInfo}
import io.iohk.node.repositories.daos.AtalaObjectsDAO
import io.iohk.node.services.models.AtalaObjectUpdate
import io.iohk.node.{AtalaReferenceLedger, objects}
import io.iohk.prism.protos.{node_internal, node_models}
import org.mockito
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.concurrent.duration._

object ObjectManagementServiceSpec {
  private val newKeysPairs = List.fill(10) { ECKeys.generateKeyPair() }

  val exampleOperations = newKeysPairs.zipWithIndex.map {
    case (keyPair: KeyPair, i) =>
      BlockProcessingServiceSpec.createDidOperation.update(_.createDid.didData.publicKeys.modify { keys =>
        keys :+ node_models.PublicKey(
          id = s"key$i",
          usage = node_models.KeyUsage.AUTHENTICATION_KEY,
          keyData = node_models.PublicKey.KeyData.EcKeyData(
            CreateDIDOperationSpec.protoECKeyFromPublicKey(keyPair.getPublic)
          )
        )
      })
  }

  val exampleSignedOperations = exampleOperations.map { operation =>
    BlockProcessingServiceSpec.signOperation(operation, "master", CreateDIDOperationSpec.masterKeys.getPrivate)
  }
}

class ObjectManagementServiceSpec extends PostgresRepositorySpec with MockitoSugar with BeforeAndAfterEach {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

  import ObjectManagementServiceSpec._

  override val tables = List("atala_objects")

  val storage = new objects.ObjectStorageService.InMemory

  val ledger: AtalaReferenceLedger = mock[AtalaReferenceLedger]
  val blockProcessing: BlockProcessingService = mock[BlockProcessingService]

  lazy val objectManagmentService = new ObjectManagementService(storage, ledger, blockProcessing)

  lazy val dummyTimestamp = TimestampInfo.dummyTime.atalaBlockTimestamp
  lazy val dummyABSequenceNumber = TimestampInfo.dummyTime.atalaBlockSequenceNumber

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(ledger)
    reset(blockProcessing)
  }

  "ObjectManagementService.publishAtalaOperation" should {
    "put reference to object onto the ledger" in {
      doReturn(Future.successful(())).when(ledger).publishReference(*)

      objectManagmentService.publishAtalaOperation(BlockProcessingServiceSpec.signedCreateDidOperation)

      val refCaptor = ArgCaptor[SHA256Digest]
      verify(ledger).publishReference(refCaptor)
      val ref = refCaptor.value

      val atalaBlock = getBlockFromStorage(ref)
      atalaBlock.operations must contain theSameElementsAs (Seq(BlockProcessingServiceSpec.signedCreateDidOperation))

      verifyNoMoreInteractions(ledger)
    }

    "put many references onto the ledger" in {
      doReturn(Future.successful(())).when(ledger).publishReference(*)

      Future
        .traverse(exampleSignedOperations) { signedOp =>
          objectManagmentService.publishAtalaOperation(signedOp)
        }
        .futureValue

      val refCaptor = ArgCaptor[SHA256Digest]
      verify(ledger, times(exampleOperations.size)).publishReference(refCaptor)

      for ((signedOp, ref) <- (exampleSignedOperations zip refCaptor.values)) {
        val atalaBlock = getBlockFromStorage(ref)
        atalaBlock.operations must contain theSameElementsAs Seq(signedOp)
      }

      verifyNoMoreInteractions(ledger)
    }
  }

  "ObjectManagementService.saveReference" should {
    "add reference to the database" in {
      val block = exampleBlock()
      val objectHash = createExampleObject(block)
      objectManagmentService.justSaveObject(AtalaObjectUpdate.Reference(objectHash), dummyTimestamp).futureValue

      val atalaObject = AtalaObjectsDAO.get(objectHash).transact(database).unsafeRunSync().value
      atalaObject.sequenceNumber mustBe 1
      atalaObject.processed mustBe false
    }

    "be idempotent - ignore re-adding the same hash" in {
      val block = exampleBlock()
      val objectHash = createExampleObject(block)
      objectManagmentService.justSaveObject(AtalaObjectUpdate.Reference(objectHash), dummyTimestamp).futureValue

      objectManagmentService.justSaveObject(AtalaObjectUpdate.Reference(objectHash), dummyTimestamp).futureValue

      val atalaObject = AtalaObjectsDAO.get(objectHash).transact(database).unsafeRunSync().value
      atalaObject.sequenceNumber mustBe 1
      atalaObject.processed mustBe false
    }

    "process the block" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)

      val block = exampleBlock()
      val objectHash = createExampleObject(block)
      objectManagmentService.saveObject(AtalaObjectUpdate.Reference(objectHash), dummyTimestamp).futureValue

      val blockCaptor = ArgCaptor[node_internal.AtalaBlock]
      verify(blockProcessing).processBlock(
        blockCaptor,
        mockito.ArgumentMatchers.eq(dummyTimestamp),
        mockito.ArgumentMatchers.eq(dummyABSequenceNumber)
      )
      blockCaptor.value mustEqual block

      verifyNoMoreInteractions(blockProcessing)

      val atalaObject = AtalaObjectsDAO.get(objectHash).transact(database).unsafeRunSync().value
      atalaObject.sequenceNumber mustBe 1
      atalaObject.processed mustBe true
    }

    "add objects by content and by reference" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*, *, *)

      val blocks = for ((signedOp, i) <- exampleSignedOperations.zipWithIndex) yield {
        val includeBlock = (i & 1) == 1
        val includeObject = (i >> 1 & 1) == 1

        val block = exampleBlock(signedOp)
        val objectUpdate = createExampleObjectUpdate(block, includeBlock, includeObject)

        objectManagmentService.saveObject(objectUpdate, Instant.ofEpochMilli(i)).futureValue

        val objectHash = objectUpdate match {
          case AtalaObjectUpdate.Reference(hash) => hash
          case AtalaObjectUpdate.ByteContent(bytes) => SHA256Digest.compute(bytes)
        }

        val atalaObject = AtalaObjectsDAO.get(objectHash).transact(database).unsafeRunSync().value
        atalaObject.sequenceNumber mustBe (i + 1)
        atalaObject.processed mustBe true
        atalaObject.byteContent.isDefined mustBe includeObject

        block
      }

      val blockCaptor = ArgCaptor[node_internal.AtalaBlock]
      verify(blockProcessing, times(blocks.size))
        .processBlock(blockCaptor, mockito.ArgumentMatchers.any(), mockito.ArgumentMatchers.any())
      blockCaptor.values must contain theSameElementsAs blocks

      verifyNoMoreInteractions(blockProcessing)
    }
  }

  protected def getBlockFromStorage(ref: SHA256Digest): node_internal.AtalaBlock = {
    val atalaObjectData = storage.get(ref.hexValue).futureValue.value
    val atalaObject = node_internal.AtalaObject.parseFrom(atalaObjectData)
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

  protected def storeObject(atalaObject: node_internal.AtalaObject): SHA256Digest = {
    val objectBytes = atalaObject.toByteArray
    val objectHash = SHA256Digest.compute(objectBytes)
    storage.put(objectHash.hexValue, objectBytes)

    objectHash
  }

  protected def createExampleObject(block: node_internal.AtalaBlock): SHA256Digest = {
    val blockBytes = block.toByteArray
    val blockHash = storeBlock(block)

    val atalaObject = node_internal.AtalaObject(
      1,
      blockBytes.length,
      node_internal.AtalaObject.Block.BlockHash(ByteString.copyFrom(blockHash.value))
    )
    storeObject(atalaObject)
  }

  protected def createExampleObjectUpdate(
      block: node_internal.AtalaBlock,
      includeBlock: Boolean,
      includeObject: Boolean
  ): AtalaObjectUpdate = {
    val blockBytes = block.toByteArray

    val atalaObject = if (includeBlock) {
      node_internal.AtalaObject(1, blockBytes.length, node_internal.AtalaObject.Block.BlockContent(block))
    } else {
      val blockHash = storeBlock(block)
      node_internal.AtalaObject(
        1,
        blockBytes.length,
        node_internal.AtalaObject.Block.BlockHash(ByteString.copyFrom(blockHash.value))
      )
    }

    if (includeObject) {
      AtalaObjectUpdate.ByteContent(atalaObject.toByteArray)
    } else {
      AtalaObjectUpdate.Reference(storeObject(atalaObject))
    }
  }

}

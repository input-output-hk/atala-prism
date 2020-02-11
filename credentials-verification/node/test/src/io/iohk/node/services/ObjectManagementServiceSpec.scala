package io.iohk.node.services

import java.security.KeyPair

import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.implicits._
import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.models.SHA256Digest
import io.iohk.node.operations.CreateDIDOperationSpec
import io.iohk.node.repositories.daos.AtalaObjectsDAO
import io.iohk.node.{AtalaReferenceLedger, objects}
import io.iohk.nodenew.atala_bitcoin_new.AtalaBlock
import io.iohk.nodenew.{atala_bitcoin_new => atala_proto, geud_node_new => geud_proto}
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.concurrent.duration._

object ObjectManagementServiceSpec {
  private val newKeysPairs = List.fill(5) { ECKeys.generateKeyPair() }

  val exampleOperations = newKeysPairs.zipWithIndex.map {
    case (keyPair: KeyPair, i) =>
      BlockProcessingServiceSpec.createDidOperation.update(_.createDid.didData.publicKeys.modify { keys =>
        keys :+ geud_proto.PublicKey(
          id = s"key$i",
          usage = geud_proto.KeyUsage.AUTHENTICATION_KEY,
          keyData = geud_proto.PublicKey.KeyData.EcKeyData(
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
      objectManagmentService.justSaveReference(objectHash).futureValue

      val atalaObject = AtalaObjectsDAO.get(objectHash).transact(database).unsafeRunSync().value
      atalaObject.sequenceNumber mustBe 1
      atalaObject.processed mustBe false
      atalaObject.blockHash mustBe None
    }

    "be idempotent - ignore re-adding the same hash" in {
      val block = exampleBlock()
      val objectHash = createExampleObject(block)
      objectManagmentService.justSaveReference(objectHash).futureValue

      objectManagmentService.justSaveReference(objectHash).futureValue

      val atalaObject = AtalaObjectsDAO.get(objectHash).transact(database).unsafeRunSync().value
      atalaObject.sequenceNumber mustBe 1
      atalaObject.processed mustBe false
      atalaObject.blockHash mustBe None
    }

    "process the block" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*)

      val block = exampleBlock()
      val objectHash = createExampleObject(block)
      objectManagmentService.saveReference(objectHash).futureValue

      val blockCaptor = ArgCaptor[AtalaBlock]
      verify(blockProcessing).processBlock(blockCaptor)
      blockCaptor.value mustEqual block

      verifyNoMoreInteractions(blockProcessing)

      val atalaObject = AtalaObjectsDAO.get(objectHash).transact(database).unsafeRunSync().value
      atalaObject.sequenceNumber mustBe 1
      atalaObject.processed mustBe true
      atalaObject.blockHash.value
    }

    "add references for many blocks" in {
      doReturn(connection.pure(true)).when(blockProcessing).processBlock(*)

      val blocks = for ((signedOp, i) <- exampleSignedOperations.zipWithIndex) yield {
        val block = exampleBlock(signedOp)
        val objectHash = createExampleObject(block)
        objectManagmentService.saveReference(objectHash).futureValue

        val atalaObject = AtalaObjectsDAO.get(objectHash).transact(database).unsafeRunSync().value
        atalaObject.sequenceNumber mustBe (i + 1)
        atalaObject.processed mustBe true
        atalaObject.blockHash.value

        block
      }

      val blockCaptor = ArgCaptor[AtalaBlock]
      verify(blockProcessing, times(blocks.size)).processBlock(blockCaptor)
      blockCaptor.values must contain theSameElementsAs blocks

      verifyNoMoreInteractions(blockProcessing)

    }
  }

  protected def getBlockFromStorage(ref: SHA256Digest): atala_proto.AtalaBlock = {
    val atalaObjectData = storage.get(ref.hexValue).value
    val atalaObject = atala_proto.AtalaObject.parseFrom(atalaObjectData)
    val atalaBlockHash = SHA256Digest(atalaObject.blockHash.toByteArray)
    val atalaBlockData = storage.get(atalaBlockHash.hexValue).value
    atala_proto.AtalaBlock.parseFrom(atalaBlockData)
  }

  protected def exampleBlock(
      signedOperation: geud_proto.SignedAtalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
  ): atala_proto.AtalaBlock = {
    atala_proto.AtalaBlock(version = "1.0", operations = Seq(signedOperation))
  }

  protected def createExampleObject(block: atala_proto.AtalaBlock): SHA256Digest = {
    val blockBytes = block.toByteArray
    val blockHash = SHA256Digest.compute(blockBytes)
    storage.put(blockHash.hexValue, blockBytes)

    val atalaObject = atala_proto.AtalaObject(ByteString.copyFrom(blockHash.value), 1, blockBytes.length)
    val objectBytes = atalaObject.toByteArray
    val objectHash = SHA256Digest.compute(objectBytes)
    storage.put(objectHash.hexValue, objectBytes)

    objectHash
  }

}

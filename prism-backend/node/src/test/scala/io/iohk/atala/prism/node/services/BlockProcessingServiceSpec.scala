package io.iohk.atala.prism.node.services

import cats.effect.IO
import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.kotlin.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.kotlin.crypto.Sha256
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.protos.models.TimestampInfo
import io.iohk.atala.prism.models.{DIDSuffix, Ledger, TransactionId}
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.models.{AtalaOperationInfo, AtalaOperationStatus}
import io.iohk.atala.prism.node.operations.{CreateDIDOperation, CreateDIDOperationSpec}
import io.iohk.atala.prism.node.operations.UpdateDIDOperationSpec.{exampleAddKeyAction, exampleRemoveKeyAction}
import io.iohk.atala.prism.node.repositories.daos.DIDDataDAO
import io.iohk.atala.prism.node.repositories.DIDDataRepository
import io.iohk.atala.prism.protos.{node_internal, node_models}
import org.scalatest.OptionValues._

import java.time.Instant

object BlockProcessingServiceSpec {
  import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.masterKeys
  val createDidOperation = CreateDIDOperationSpec.exampleOperation

  def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      key: ECPrivateKey
  ): node_models.SignedAtalaOperation = {
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(EC.signBytes(operation.toByteArray, key).getData)
    )
  }

  val signedCreateDidOperation = signOperation(createDidOperation, "master", masterKeys.getPrivateKey)
  val signedCreateDidOperationId = AtalaOperationId.of(signedCreateDidOperation)

  val exampleBlock = node_internal.AtalaBlock(
    operations = Seq(signedCreateDidOperation)
  )

}

class BlockProcessingServiceSpec extends AtalaWithPostgresSpec {

  import BlockProcessingServiceSpec._
  import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.masterKeys

  lazy val didDataRepository: DIDDataRepository[IO] = DIDDataRepository(database)

  private val dummyTimestampInfo = new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)

  val service = new BlockProcessingServiceImpl()
  val dummyTimestamp = Instant.ofEpochMilli(dummyTimestampInfo.getAtalaBlockTimestamp)
  val dummyTransactionId = TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value
  val dummyLedger = Ledger.InMemory
  val dummyABSequenceNumber = dummyTimestampInfo.getAtalaBlockSequenceNumber

  "BlockProcessingService" should {
    "apply block in" in {
      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        exampleBlock.operations.toList,
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(1)
      val atalaOperationId = opIds.head

      val result = service
        .processBlock(exampleBlock, dummyTransactionId, dummyLedger, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      credentials.size mustBe 1
      val digest = Sha256.compute(createDidOperation.toByteArray)
      credentials.head mustBe DIDSuffix(digest.getHexValue)

      val atalaOperationInfo = DataPreparation.getOperationInfo(atalaOperationId).value
      val expectedAtalaOperationInfo = AtalaOperationInfo(atalaOperationId, objId, AtalaOperationStatus.APPLIED, None)
      atalaOperationInfo must be(expectedAtalaOperationInfo)
    }

    "apply block received by other node instance" in {
      val result = service
        .processBlock(exampleBlock, dummyTransactionId, dummyLedger, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      credentials.size mustBe 1
      val digest = Sha256.compute(createDidOperation.toByteArray)
      credentials.head mustBe DIDSuffix(digest.getHexValue)

      // shouldn't add new operations to the table
      val count = DataPreparation.getOperationsCount()
      count must be(0)
    }

    "not apply operation when signature is wrong" in {
      val invalidSignatureOperation = signedCreateDidOperation.withSignature(ByteString.EMPTY)

      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(invalidSignatureOperation),
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(1)
      val atalaOperationId = opIds.head

      val invalidSignatureBlock = node_internal.AtalaBlock(
        operations = Seq(invalidSignatureOperation)
      )

      val result = service
        .processBlock(invalidSignatureBlock, dummyTransactionId, dummyLedger, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val atalaOperationInfo = DataPreparation.getOperationInfo(atalaOperationId).value
      val expectedAtalaOperationInfo = AtalaOperationInfo(atalaOperationId, objId, AtalaOperationStatus.REJECTED, None)
      atalaOperationInfo must be(expectedAtalaOperationInfo)
    }

    "ignore block when it contains invalid operations" in {
      val invalidOperation = createDidOperation.update(_.createDid.didData.id := "id")
      val signedInvalidOperation = signOperation(invalidOperation, "master", masterKeys.getPrivateKey)

      val invalidBlock = node_internal.AtalaBlock(
        operations = Seq(signedInvalidOperation)
      )
      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(signedInvalidOperation),
        AtalaOperationStatus.RECEIVED
      )

      opIds.size must be(1)
      val atalaOperationId = opIds.head

      val result = service
        .processBlock(invalidBlock, dummyTransactionId, dummyLedger, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe false

      val atalaOperationInfo = DataPreparation.getOperationInfo(atalaOperationId).value
      val expectedAtalaOperationInfo = AtalaOperationInfo(atalaOperationId, objId, AtalaOperationStatus.REJECTED, None)
      atalaOperationInfo must be(expectedAtalaOperationInfo)
    }

    "apply correct operations even though there are incorrect ones in the block" in {
      val signedOperation1 = signedCreateDidOperation

      val operation2 = createDidOperation
        .copy()
        .update(
          _.createDid.didData.publicKeys := List(
            node_models.PublicKey(
              "master",
              node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData
                .EcKeyData(CreateDIDOperationSpec.protoECKeyDataFromPublicKey(EC.generateKeyPair().getPublicKey))
            )
          )
        )
      val incorrectlySignedOperation2 = signOperation(operation2, "master", masterKeys.getPrivateKey)

      val operation3Keys = EC.generateKeyPair()
      val operation3 = createDidOperation
        .copy()
        .update(
          _.createDid.didData.publicKeys := List(
            node_models.PublicKey(
              "rootkey",
              node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData
                .EcKeyData(CreateDIDOperationSpec.protoECKeyDataFromPublicKey(operation3Keys.getPublicKey))
            )
          )
        )
      val signedOperation3 = signOperation(operation3, "rootkey", operation3Keys.getPrivateKey)

      val block = node_internal.AtalaBlock(
        operations = Seq(signedOperation1, incorrectlySignedOperation2, signedOperation3)
      )

      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(signedOperation1, incorrectlySignedOperation2, signedOperation3),
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(3)

      val result = service
        .processBlock(block, dummyTransactionId, dummyLedger, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      val expectedSuffixes = Seq(signedOperation1.getOperation, operation3)
        .map(op => DIDSuffix(Sha256.compute(op.toByteArray).getHexValue))
      credentials must contain theSameElementsAs (expectedSuffixes)

      val atalaOperationInfo1 = DataPreparation.getOperationInfo(opIds.head).value
      val expectedAtalaOperationInfo1 = AtalaOperationInfo(opIds.head, objId, AtalaOperationStatus.APPLIED, None)
      atalaOperationInfo1 must be(expectedAtalaOperationInfo1)

      val atalaOperationInfo2 = DataPreparation.getOperationInfo(opIds(1)).value
      val expectedAtalaOperationInfo2 = AtalaOperationInfo(opIds(1), objId, AtalaOperationStatus.REJECTED, None)
      atalaOperationInfo2 must be(expectedAtalaOperationInfo2)

      val atalaOperationInfo3 = DataPreparation.getOperationInfo(opIds.last).value
      val expectedAtalaOperationInfo3 = AtalaOperationInfo(opIds.last, objId, AtalaOperationStatus.APPLIED, None)
      atalaOperationInfo3 must be(expectedAtalaOperationInfo3)
    }

    "apply two update operations sequentially" in {
      val did = CreateDIDOperation
        .parse(CreateDIDOperationSpec.exampleOperation, CreateDIDOperationSpec.dummyLedgerData)
        .toOption
        .value
        .id
        .getValue

      val createDidSignedOperation = signedCreateDidOperation

      val updateDidOperation1 = node_models.AtalaOperation(
        operation = node_models.AtalaOperation.Operation.UpdateDid(
          value = node_models.UpdateDIDOperation(
            previousOperationHash = ByteString.copyFrom(
              Sha256.compute(signedCreateDidOperation.operation.value.toByteArray).getValue
            ),
            id = did,
            actions = Seq(exampleAddKeyAction)
          )
        )
      )
      val updateDidSignedOperation1 = signOperation(updateDidOperation1, "master", masterKeys.getPrivateKey)

      val updateDidOperation2 = node_models.AtalaOperation(
        operation = node_models.AtalaOperation.Operation.UpdateDid(
          value = node_models.UpdateDIDOperation(
            previousOperationHash = ByteString.copyFrom(Sha256.compute(updateDidOperation1.toByteArray).getValue),
            id = did,
            actions = Seq(exampleRemoveKeyAction)
          )
        )
      )
      val updateDidSignedOperation2 = signOperation(updateDidOperation2, "master", masterKeys.getPrivateKey)

      val block = node_internal.AtalaBlock(
        operations = Seq(createDidSignedOperation, updateDidSignedOperation1, updateDidSignedOperation2)
      )

      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(createDidSignedOperation, updateDidSignedOperation1, updateDidSignedOperation2),
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(3)

      val result = service
        .processBlock(block, dummyTransactionId, dummyLedger, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val createDidSignedOperationInfo = DataPreparation.getOperationInfo(opIds.head).value
      val expectedAtalaOperationInfo1 = AtalaOperationInfo(opIds.head, objId, AtalaOperationStatus.APPLIED, None)
      createDidSignedOperationInfo must be(expectedAtalaOperationInfo1)

      val updateDidSignedOperation1Info = DataPreparation.getOperationInfo(opIds(1)).value
      val expectedAtalaOperationInfo2 = AtalaOperationInfo(opIds(1), objId, AtalaOperationStatus.APPLIED, None)
      updateDidSignedOperation1Info must be(expectedAtalaOperationInfo2)

      val updateDidSignedOperation2Info = DataPreparation.getOperationInfo(opIds.last).value
      val expectedAtalaOperationInfo3 = AtalaOperationInfo(opIds.last, objId, AtalaOperationStatus.APPLIED, None)
      updateDidSignedOperation2Info must be(expectedAtalaOperationInfo3)
    }

    "skip duplicate operations" in {
      val signedOperation1 = signedCreateDidOperation

      val operation2 = createDidOperation
        .copy()
        .update(
          _.createDid.didData.publicKeys := List(
            node_models.PublicKey(
              "master",
              node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData
                .EcKeyData(CreateDIDOperationSpec.protoECKeyDataFromPublicKey(EC.generateKeyPair().getPublicKey))
            )
          )
        )
      val incorrectlySignedOperation2 = signOperation(operation2, "master", masterKeys.getPrivateKey)

      val operation3Keys = EC.generateKeyPair()
      val operation3 = createDidOperation
        .copy()
        .update(
          _.createDid.didData.publicKeys := List(
            node_models.PublicKey(
              "rootkey",
              node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData
                .EcKeyData(CreateDIDOperationSpec.protoECKeyDataFromPublicKey(operation3Keys.getPublicKey))
            )
          )
        )
      val signedOperation3 = signOperation(operation3, "rootkey", operation3Keys.getPrivateKey)

      val block1 = node_internal.AtalaBlock( // first block contains 1 valid and 1 invalid operation
        operations = Seq(signedOperation1, incorrectlySignedOperation2)
      )
      val block2 = node_internal.AtalaBlock( // second block contains 1 valid operation, and two duplications
        operations = Seq(signedOperation3, signedOperation1, signedOperation3)
      )
      val block3 = node_internal.AtalaBlock( // third block contains 1 duplicate operation and 1 invalid
        operations = Seq(signedOperation1, incorrectlySignedOperation2)
      )

      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(signedOperation1, incorrectlySignedOperation2, signedOperation3),
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(3)

      val result1 = service
        .processBlock(block1, dummyTransactionId, dummyLedger, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue
      result1 mustBe true

      val result2 = service
        .processBlock(block2, dummyTransactionId, dummyLedger, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue
      result2 mustBe true

      val result3 = service
        .processBlock(block3, dummyTransactionId, dummyLedger, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue
      result3 mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      val expectedSuffixes = Seq(signedOperation1.getOperation, operation3)
        .map(op => DIDSuffix(Sha256.compute(op.toByteArray).getHexValue))
      credentials must contain theSameElementsAs (expectedSuffixes)

      val atalaOperationInfo1 = DataPreparation.getOperationInfo(opIds.head).value
      val expectedAtalaOperationInfo1 = AtalaOperationInfo(opIds.head, objId, AtalaOperationStatus.APPLIED, None)
      atalaOperationInfo1 must be(expectedAtalaOperationInfo1)

      val atalaOperationInfo2 = DataPreparation.getOperationInfo(opIds(1)).value
      val expectedAtalaOperationInfo2 = AtalaOperationInfo(opIds(1), objId, AtalaOperationStatus.REJECTED, None)
      atalaOperationInfo2 must be(expectedAtalaOperationInfo2)

      val atalaOperationInfo3 = DataPreparation.getOperationInfo(opIds.last).value
      val expectedAtalaOperationInfo3 = AtalaOperationInfo(opIds.last, objId, AtalaOperationStatus.APPLIED, None)
      atalaOperationInfo3 must be(expectedAtalaOperationInfo3)
    }
  }
}

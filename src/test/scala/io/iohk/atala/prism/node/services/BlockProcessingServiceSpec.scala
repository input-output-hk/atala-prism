package io.iohk.atala.prism.node.services

import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpECDSA, SecpPrivateKey, Sha256Hash}
import io.iohk.atala.prism.node.models.TimestampInfo
import io.iohk.atala.prism.node.models.{AtalaOperationId, DidSuffix, Ledger, TransactionId}
import io.iohk.atala.prism.node.{AtalaWithPostgresSpec, DataPreparation}
import io.iohk.atala.prism.node.models.{AtalaOperationInfo, AtalaOperationStatus}
import io.iohk.atala.prism.node.operations.{
  ApplyOperationConfig,
  CreateDIDOperation,
  CreateDIDOperationSpec,
  UpdateDIDOperationSpec
}
import io.iohk.atala.prism.node.operations.UpdateDIDOperationSpec.{exampleAddKeyAction, exampleRemoveKeyAction}
import io.iohk.atala.prism.node.repositories.daos.DIDDataDAO
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.node_models
import org.scalatest.OptionValues._

import java.time.Instant

object BlockProcessingServiceSpec {
  import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.masterKeys
  val createDidOperation = CreateDIDOperationSpec.exampleOperation
  val updateDidOperation = UpdateDIDOperationSpec.exampleAddAndRemoveOperation

  def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      key: SecpPrivateKey
  ): node_models.SignedAtalaOperation = {
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(SecpECDSA.signBytes(operation.toByteArray, key).bytes)
    )
  }

  val signedCreateDidOperation =
    signOperation(createDidOperation, "master", masterKeys.privateKey)
  val signedCreateDidOperationId = AtalaOperationId.of(signedCreateDidOperation)

  val signedUpdateDidOperation: SignedAtalaOperation =
    signOperation(updateDidOperation, "master", masterKeys.privateKey)

  val exampleBlock = node_models.AtalaBlock(
    operations = Seq(signedCreateDidOperation)
  )

}

class BlockProcessingServiceSpec extends AtalaWithPostgresSpec {

  import BlockProcessingServiceSpec._
  import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.masterKeys

  private val dummyTimestampInfo =
    new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)

  val service = new BlockProcessingServiceImpl(ApplyOperationConfig(DidSuffix("012ae")))
  val dummyTimestamp =
    Instant.ofEpochMilli(dummyTimestampInfo.atalaBlockTimestamp)
  val dummyTransactionId = TransactionId
    .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
    .value
  val dummyLedger = Ledger.InMemory
  val dummyABSequenceNumber = dummyTimestampInfo.atalaBlockSequenceNumber

  "BlockProcessingService" should {
    "apply block in" in {
      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        exampleBlock.operations.toList,
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(1)
      val atalaOperationId = opIds.head

      val result = service
        .processBlock(
          exampleBlock,
          dummyTransactionId,
          dummyLedger,
          dummyTimestamp,
          dummyABSequenceNumber
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      credentials.size mustBe 1
      val digest = Sha256Hash.compute(createDidOperation.toByteArray)
      credentials.head mustBe DidSuffix(digest.hexEncoded)

      val atalaOperationInfo =
        DataPreparation.getOperationInfo(atalaOperationId).value
      val expectedAtalaOperationInfo = AtalaOperationInfo(
        atalaOperationId,
        objId,
        AtalaOperationStatus.APPLIED,
        "",
        None
      )
      atalaOperationInfo must be(expectedAtalaOperationInfo)
    }

    "apply block received by other node instance" in {
      val result = service
        .processBlock(
          exampleBlock,
          dummyTransactionId,
          dummyLedger,
          dummyTimestamp,
          dummyABSequenceNumber
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      credentials.size mustBe 1
      val digest = Sha256Hash.compute(createDidOperation.toByteArray)
      credentials.head mustBe DidSuffix(digest.hexEncoded)

      // shouldn't add new operations to the table
      val count = DataPreparation.getOperationsCount()
      count must be(0)
    }

    "not apply operation when entity is missing" in {
      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(signedUpdateDidOperation),
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(1)
      val atalaOperationId = opIds.head

      val atalaBlock = node_models.AtalaBlock(
        operations = Seq(signedUpdateDidOperation)
      )

      val result = service
        .processBlock(
          atalaBlock,
          dummyTransactionId,
          dummyLedger,
          dummyTimestamp,
          dummyABSequenceNumber
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val atalaOperationInfo =
        DataPreparation.getOperationInfo(atalaOperationId).value
      val expectedAtalaOperationInfo = AtalaOperationInfo(
        atalaOperationId,
        objId,
        AtalaOperationStatus.REJECTED,
        s"EntityMissing(did suffix,${signedUpdateDidOperation.getOperation.getUpdateDid.id})",
        None
      )
      atalaOperationInfo must be(expectedAtalaOperationInfo)
    }

    "not apply operation when signature is wrong" in {
      val invalidSignatureOperation =
        signedCreateDidOperation.withSignature(ByteString.EMPTY)

      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(invalidSignatureOperation),
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(1)
      val atalaOperationId = opIds.head

      val invalidSignatureBlock = node_models.AtalaBlock(
        operations = Seq(invalidSignatureOperation)
      )

      val result = service
        .processBlock(
          invalidSignatureBlock,
          dummyTransactionId,
          dummyLedger,
          dummyTimestamp,
          dummyABSequenceNumber
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val atalaOperationInfo =
        DataPreparation.getOperationInfo(atalaOperationId).value
      val expectedAtalaOperationInfo = AtalaOperationInfo(
        atalaOperationId,
        objId,
        AtalaOperationStatus.REJECTED,
        "InvalidSignature()",
        None
      )
      atalaOperationInfo must be(expectedAtalaOperationInfo)
    }

    "ignore invalid operations in the block and process valid ones" in {
      val invalidOperation =
        updateDidOperation.update(_.updateDid.actions(0).addKey.key.id := "")
      val signedInvalidOperation =
        signOperation(invalidOperation, "master", masterKeys.privateKey)

      val signedValidOperation = signOperation(createDidOperation, "master", masterKeys.privateKey)

      val block = node_models.AtalaBlock(
        operations = Seq(signedInvalidOperation, signedValidOperation)
      )
      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(signedInvalidOperation, signedValidOperation),
        AtalaOperationStatus.RECEIVED
      )

      opIds.size must be(2)
      val validOperationId = AtalaOperationId.of(signedValidOperation)
      val invalidOperationId = AtalaOperationId.of(signedInvalidOperation)

      val result = service
        .processBlock(
          block,
          dummyTransactionId,
          dummyLedger,
          dummyTimestamp,
          dummyABSequenceNumber
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue

      // the block is processed, because at least one operation inside of it is processed.
      result mustBe true

      // invalid op should be rejected
      val invalidAtalaOperationInfo =
        DataPreparation.getOperationInfo(invalidOperationId).value
      val expectedInvalidAtalaOperationInfo = AtalaOperationInfo(
        invalidOperationId,
        objId,
        AtalaOperationStatus.REJECTED,
        "",
        None
      )
      invalidAtalaOperationInfo must be(expectedInvalidAtalaOperationInfo)

      // valid op should be applied
      val validAtalaOperationInfo =
        DataPreparation.getOperationInfo(validOperationId).value
      val expectedValidAtalaOperationInfo = AtalaOperationInfo(
        validOperationId,
        objId,
        AtalaOperationStatus.APPLIED,
        "",
        None
      )
      validAtalaOperationInfo must be(expectedValidAtalaOperationInfo)
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
                .CompressedEcKeyData(
                  CreateDIDOperationSpec.protoCompressedECKeyDataFromPublicKey(
                    CryptoTestUtils.generateKeyPair().publicKey
                  )
                )
            )
          )
        )
      val incorrectlySignedOperation2 =
        signOperation(operation2, "master", masterKeys.privateKey)

      val operation3Keys = CryptoTestUtils.generateKeyPair()
      val operation3 = createDidOperation
        .copy()
        .update(
          _.createDid.didData.publicKeys := List(
            node_models.PublicKey(
              "rootkey",
              node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData
                .CompressedEcKeyData(
                  CreateDIDOperationSpec
                    .protoCompressedECKeyDataFromPublicKey(operation3Keys.publicKey)
                )
            )
          )
        )
      val signedOperation3 =
        signOperation(operation3, "rootkey", operation3Keys.privateKey)

      val block = node_models.AtalaBlock(
        operations = Seq(signedOperation1, incorrectlySignedOperation2, signedOperation3)
      )

      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(signedOperation1, incorrectlySignedOperation2, signedOperation3),
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(3)

      val result = service
        .processBlock(
          block,
          dummyTransactionId,
          dummyLedger,
          dummyTimestamp,
          dummyABSequenceNumber
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      val expectedSuffixes = Seq(signedOperation1.getOperation, operation3)
        .map(op => DidSuffix(Sha256Hash.compute(op.toByteArray).hexEncoded))
      credentials must contain theSameElementsAs (expectedSuffixes)

      val atalaOperationInfo1 =
        DataPreparation.getOperationInfo(opIds.head).value
      val expectedAtalaOperationInfo1 = AtalaOperationInfo(
        opIds.head,
        objId,
        AtalaOperationStatus.APPLIED,
        "",
        None
      )
      atalaOperationInfo1 must be(expectedAtalaOperationInfo1)

      val atalaOperationInfo2 = DataPreparation.getOperationInfo(opIds(1)).value
      val expectedAtalaOperationInfo2 =
        AtalaOperationInfo(opIds(1), objId, AtalaOperationStatus.REJECTED, "InvalidSignature()", None)
      atalaOperationInfo2 must be(expectedAtalaOperationInfo2)

      val atalaOperationInfo3 =
        DataPreparation.getOperationInfo(opIds.last).value
      val expectedAtalaOperationInfo3 = AtalaOperationInfo(
        opIds.last,
        objId,
        AtalaOperationStatus.APPLIED,
        "",
        None
      )
      atalaOperationInfo3 must be(expectedAtalaOperationInfo3)
    }

    "apply two update operations sequentially" in {
      val did = CreateDIDOperation
        .parse(
          CreateDIDOperationSpec.exampleOperation,
          DataPreparation.dummyLedgerData
        )
        .toOption
        .value
        .id
        .getValue

      val createDidSignedOperation = signedCreateDidOperation

      val updateDidOperation1 = node_models.AtalaOperation(
        operation = node_models.AtalaOperation.Operation.UpdateDid(
          value = node_models.UpdateDIDOperation(
            previousOperationHash = ByteString.copyFrom(
              Sha256Hash
                .compute(signedCreateDidOperation.operation.value.toByteArray)
                .bytes
                .toArray
            ),
            id = did,
            actions = Seq(exampleAddKeyAction)
          )
        )
      )
      val updateDidSignedOperation1 =
        signOperation(updateDidOperation1, "master", masterKeys.privateKey)

      val updateDidOperation2 = node_models.AtalaOperation(
        operation = node_models.AtalaOperation.Operation.UpdateDid(
          value = node_models.UpdateDIDOperation(
            previousOperationHash = ByteString.copyFrom(
              Sha256Hash.compute(updateDidOperation1.toByteArray).bytes.toArray
            ),
            id = did,
            actions = Seq(exampleRemoveKeyAction)
          )
        )
      )
      val updateDidSignedOperation2 =
        signOperation(updateDidOperation2, "master", masterKeys.privateKey)

      val block = node_models.AtalaBlock(
        operations = Seq(
          createDidSignedOperation,
          updateDidSignedOperation1,
          updateDidSignedOperation2
        )
      )

      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(
          createDidSignedOperation,
          updateDidSignedOperation1,
          updateDidSignedOperation2
        ),
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(3)

      val result = service
        .processBlock(
          block,
          dummyTransactionId,
          dummyLedger,
          dummyTimestamp,
          dummyABSequenceNumber
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val createDidSignedOperationInfo =
        DataPreparation.getOperationInfo(opIds.head).value
      val expectedAtalaOperationInfo1 = AtalaOperationInfo(
        opIds.head,
        objId,
        AtalaOperationStatus.APPLIED,
        "",
        None
      )
      createDidSignedOperationInfo must be(expectedAtalaOperationInfo1)

      val updateDidSignedOperation1Info =
        DataPreparation.getOperationInfo(opIds(1)).value
      val expectedAtalaOperationInfo2 =
        AtalaOperationInfo(opIds(1), objId, AtalaOperationStatus.APPLIED, "", None)
      updateDidSignedOperation1Info must be(expectedAtalaOperationInfo2)

      val updateDidSignedOperation2Info =
        DataPreparation.getOperationInfo(opIds.last).value
      val expectedAtalaOperationInfo3 = AtalaOperationInfo(
        opIds.last,
        objId,
        AtalaOperationStatus.APPLIED,
        "",
        None
      )
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
                .CompressedEcKeyData(
                  CreateDIDOperationSpec.protoCompressedECKeyDataFromPublicKey(
                    CryptoTestUtils.generateKeyPair().publicKey
                  )
                )
            )
          )
        )
      val incorrectlySignedOperation2 =
        signOperation(operation2, "master", masterKeys.privateKey)

      val operation3Keys = CryptoTestUtils.generateKeyPair()
      val operation3 = createDidOperation
        .copy()
        .update(
          _.createDid.didData.publicKeys := List(
            node_models.PublicKey(
              "rootkey",
              node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData
                .CompressedEcKeyData(
                  CreateDIDOperationSpec
                    .protoCompressedECKeyDataFromPublicKey(operation3Keys.publicKey)
                )
            )
          )
        )
      val signedOperation3 =
        signOperation(operation3, "rootkey", operation3Keys.privateKey)

      val block1 = node_models
        .AtalaBlock( // first block contains 1 valid and 1 invalid operation
          operations = Seq(signedOperation1, incorrectlySignedOperation2)
        )
      val block2 = node_models
        .AtalaBlock( // second block contains 1 valid operation, and two duplications
          operations = Seq(signedOperation3, signedOperation1, signedOperation3)
        )
      val block3 = node_models
        .AtalaBlock( // third block contains 1 duplicate operation and 1 invalid
          operations = Seq(signedOperation1, incorrectlySignedOperation2)
        )

      val (objId, opIds) = DataPreparation.insertOperationStatuses(
        List(signedOperation1, incorrectlySignedOperation2, signedOperation3),
        AtalaOperationStatus.RECEIVED
      )
      opIds.size must be(3)

      val result1 = service
        .processBlock(
          block1,
          dummyTransactionId,
          dummyLedger,
          dummyTimestamp,
          dummyABSequenceNumber
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue
      result1 mustBe true

      val result2 = service
        .processBlock(
          block2,
          dummyTransactionId,
          dummyLedger,
          dummyTimestamp,
          dummyABSequenceNumber
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue
      result2 mustBe true

      val result3 = service
        .processBlock(
          block3,
          dummyTransactionId,
          dummyLedger,
          dummyTimestamp,
          dummyABSequenceNumber
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue
      result3 mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      val expectedSuffixes = Seq(signedOperation1.getOperation, operation3)
        .map(op => DidSuffix(Sha256Hash.compute(op.toByteArray).hexEncoded))
      credentials must contain theSameElementsAs (expectedSuffixes)

      val atalaOperationInfo1 =
        DataPreparation.getOperationInfo(opIds.head).value
      val expectedAtalaOperationInfo1 = AtalaOperationInfo(
        opIds.head,
        objId,
        AtalaOperationStatus.APPLIED,
        "",
        None
      )
      atalaOperationInfo1 must be(expectedAtalaOperationInfo1)

      val atalaOperationInfo2 = DataPreparation.getOperationInfo(opIds(1)).value
      val expectedAtalaOperationInfo2 =
        AtalaOperationInfo(opIds(1), objId, AtalaOperationStatus.REJECTED, "InvalidSignature()", None)
      atalaOperationInfo2 must be(expectedAtalaOperationInfo2)

      val atalaOperationInfo3 =
        DataPreparation.getOperationInfo(opIds.last).value
      val expectedAtalaOperationInfo3 = AtalaOperationInfo(
        opIds.last,
        objId,
        AtalaOperationStatus.APPLIED,
        "",
        None
      )
      atalaOperationInfo3 must be(expectedAtalaOperationInfo3)
    }
  }
}

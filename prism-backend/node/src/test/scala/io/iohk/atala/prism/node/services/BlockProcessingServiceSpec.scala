package io.iohk.atala.prism.node.services

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.{EC, ECPrivateKey, SHA256Digest}
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.models.{AtalaOperationInfo, AtalaOperationStatus}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec
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
      signature = ByteString.copyFrom(EC.sign(operation.toByteArray, key).data)
    )
  }

  val signedCreateDidOperation = signOperation(createDidOperation, "master", masterKeys.privateKey)
  val signedCreateDidOperationId = AtalaOperationId.of(signedCreateDidOperation)

  val exampleBlock = node_internal.AtalaBlock(
    operations = Seq(signedCreateDidOperation)
  )

}

class BlockProcessingServiceSpec extends AtalaWithPostgresSpec {

  import BlockProcessingServiceSpec._
  import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.masterKeys

  lazy val didDataRepository = new DIDDataRepository(database)

  private val dummyTimestampInfo = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)

  val service = new BlockProcessingServiceImpl()
  val dummyTimestamp = dummyTimestampInfo.atalaBlockTimestamp
  val dummyTransactionId = TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value
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
        .processBlock(exampleBlock, dummyTransactionId, dummyLedger, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      credentials.size mustBe 1
      val digest = SHA256Digest.compute(createDidOperation.toByteArray)
      credentials.head mustBe DIDSuffix.unsafeFromDigest(digest)

      val atalaOperationInfo = DataPreparation.getOperationInfo(atalaOperationId).value
      val expectedAtalaOperationInfo = AtalaOperationInfo(atalaOperationId, objId, AtalaOperationStatus.APPLIED, None)
      atalaOperationInfo must be(expectedAtalaOperationInfo)
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
      val signedInvalidOperation = signOperation(invalidOperation, "master", masterKeys.privateKey)

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
                .EcKeyData(CreateDIDOperationSpec.protoECKeyFromPublicKey(EC.generateKeyPair().publicKey))
            )
          )
        )
      val incorrectlySignedOperation2 = signOperation(operation2, "master", masterKeys.privateKey)

      val operation3Keys = EC.generateKeyPair()
      val operation3 = createDidOperation
        .copy()
        .update(
          _.createDid.didData.publicKeys := List(
            node_models.PublicKey(
              "rootkey",
              node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData
                .EcKeyData(CreateDIDOperationSpec.protoECKeyFromPublicKey(operation3Keys.publicKey))
            )
          )
        )
      val signedOperation3 = signOperation(operation3, "rootkey", operation3Keys.privateKey)

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
        .map(op => DIDSuffix.unsafeFromDigest(SHA256Digest.compute(op.toByteArray)))
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

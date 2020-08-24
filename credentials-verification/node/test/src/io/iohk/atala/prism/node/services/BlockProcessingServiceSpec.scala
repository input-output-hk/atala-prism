package io.iohk.atala.prism.node.services

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.crypto.{EC, ECPrivateKey}
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.node.models.DIDSuffix
import io.iohk.atala.prism.node.operations.{CreateDIDOperationSpec, TimestampInfo}
import io.iohk.atala.prism.node.repositories.daos.{CredentialsDAO, DIDDataDAO}
import io.iohk.atala.prism.node.repositories.{CredentialsRepository, DIDDataRepository}
import io.iohk.prism.protos.{node_internal, node_models}

import scala.concurrent.duration._

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

  val exampleBlock = node_internal.AtalaBlock(
    operations = Seq(signedCreateDidOperation)
  )

}

class BlockProcessingServiceSpec extends PostgresRepositorySpec {

  import BlockProcessingServiceSpec._
  import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.masterKeys

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  val service = new BlockProcessingServiceImpl()
  val dummyTimestamp = TimestampInfo.dummyTime.atalaBlockTimestamp
  val dummyABSequenceNumber = TimestampInfo.dummyTime.atalaBlockSequenceNumber

  "BlockProcessingService" should {
    "apply block in" in {
      val result = service
        .processBlock(exampleBlock, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      credentials.size mustBe 1
      val digest = SHA256Digest.compute(createDidOperation.toByteArray)
      credentials.head mustBe DIDSuffix(digest)
    }

    "not apply operation when signature is wrong" in {
      val invalidSignatureOperation = signedCreateDidOperation.withSignature(ByteString.EMPTY)

      val invalidSignatureBlock = node_internal.AtalaBlock(
        operations = Seq(invalidSignatureOperation)
      )

      val result = service
        .processBlock(invalidSignatureBlock, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = CredentialsDAO.all().transact(database).unsafeRunSync()
      credentials must be(empty)

    }

    "ignore block when it contains invalid operations" in {
      val invalidOperation = createDidOperation.update(_.createDid.didData.id := "id")
      val signedInvalidOperation = signOperation(invalidOperation, "master", masterKeys.privateKey)

      val invalidBlock = node_internal.AtalaBlock(
        operations = Seq(signedInvalidOperation)
      )

      val result = service
        .processBlock(invalidBlock, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe false
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

      val result = service
        .processBlock(block, dummyTimestamp, dummyABSequenceNumber)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = DIDDataDAO.all().transact(database).unsafeRunSync()
      val expectedSuffixes = Seq(signedOperation1.getOperation, operation3)
        .map(op => DIDSuffix(SHA256Digest.compute(op.toByteArray)))
      credentials must contain theSameElementsAs (expectedSuffixes)
    }
  }
}

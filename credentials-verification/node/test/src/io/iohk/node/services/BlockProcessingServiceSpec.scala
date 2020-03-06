package io.iohk.node.services

import java.security.PrivateKey

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.models.{DIDSuffix, SHA256Digest}
import io.iohk.node.operations.CreateDIDOperationSpec
import io.iohk.node.repositories.daos.{CredentialsDAO, DIDDataDAO}
import io.iohk.node.repositories.{CredentialsRepository, DIDDataRepository}
import io.iohk.prism.protos.{node_internal, node_models}

import scala.concurrent.duration._

object BlockProcessingServiceSpec {
  import io.iohk.node.operations.CreateDIDOperationSpec.masterKeys
  val createDidOperation = io.iohk.node.operations.CreateDIDOperationSpec.exampleOperation

  def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      key: PrivateKey
  ): node_models.SignedAtalaOperation = {
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(ECSignature.sign(key, operation.toByteArray).toArray)
    )
  }

  val signedCreateDidOperation = signOperation(createDidOperation, "master", masterKeys.getPrivate)

  val exampleBlock = node_internal.AtalaBlock(
    operations = Seq(signedCreateDidOperation)
  )

}

class BlockProcessingServiceSpec extends PostgresRepositorySpec {

  import BlockProcessingServiceSpec._
  import io.iohk.node.operations.CreateDIDOperationSpec.masterKeys

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  override val tables = List("credentials", "public_keys", "did_data")

  val service = new BlockProcessingServiceImpl()

  "BlockProcessingService" should {
    "apply block in" in {
      val result = service
        .processBlock(exampleBlock)
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
        .processBlock(invalidSignatureBlock)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true

      val credentials = CredentialsDAO.all().transact(database).unsafeRunSync()
      credentials must be(empty)

    }

    "ignore block when it contains invalid operations" in {
      val invalidOperation = createDidOperation.update(_.createDid.didData.id := "id")
      val signedInvalidOperation = signOperation(invalidOperation, "master", masterKeys.getPrivate)

      val invalidBlock = node_internal.AtalaBlock(
        operations = Seq(signedInvalidOperation)
      )

      val result = service
        .processBlock(invalidBlock)
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
              node_models.PublicKey.KeyData
                .EcKeyData(CreateDIDOperationSpec.protoECKeyFromPublicKey(ECKeys.generateKeyPair().getPublic))
            )
          )
        )
      val incorrectlySignedOperation2 = signOperation(operation2, "master", masterKeys.getPrivate)

      val operation3Keys = ECKeys.generateKeyPair()
      val operation3 = createDidOperation
        .copy()
        .update(
          _.createDid.didData.publicKeys := List(
            node_models.PublicKey(
              "rootkey",
              node_models.KeyUsage.MASTER_KEY,
              node_models.PublicKey.KeyData
                .EcKeyData(CreateDIDOperationSpec.protoECKeyFromPublicKey(operation3Keys.getPublic))
            )
          )
        )
      val signedOperation3 = signOperation(operation3, "rootkey", operation3Keys.getPrivate)

      val block = node_internal.AtalaBlock(
        operations = Seq(signedOperation1, incorrectlySignedOperation2, signedOperation3)
      )

      val result = service
        .processBlock(block)
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

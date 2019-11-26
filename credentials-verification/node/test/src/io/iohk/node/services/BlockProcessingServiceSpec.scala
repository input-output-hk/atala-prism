package io.iohk.node.services

import doobie.implicits._
import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECSignature
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.repositories.{CredentialsRepository, DIDDataRepository}

import scala.concurrent.duration._
import io.iohk.nodenew.{geud_node_new => geud_proto}
import io.iohk.nodenew.{atala_bitcoin_new => atala_proto}

class BlockProcessingServiceSpec extends PostgresRepositorySpec {
  import io.iohk.node.operations.CreateDIDOperationSpec.masterKeys
  val createDidOperation = io.iohk.node.operations.CreateDIDOperationSpec.exampleOperation

  val signedCreateDidOperation = geud_proto.SignedAtalaOperation(
    signedWith = "master",
    operation = Some(createDidOperation),
    signature = ByteString.copyFrom(ECSignature.sign(masterKeys.getPrivate, createDidOperation.toByteArray).toArray)
  )

  val exampleBlock = atala_proto.AtalaBlock(
    operations = Seq(signedCreateDidOperation)
  )

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  override val tables = List("credentials", "public_keys", "did_data")

  val service = new BlockProcessingService()

  "BlockProcessingService" should {
    "apply block in" in {
      val result = service
        .processBlock(exampleBlock)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe true
    }

    "ignore block when it contains invalid operations" in {
      val invalidOperation = createDidOperation.update(_.createDid.didData.id := "id")
      val signedInvalidOperation = geud_proto.SignedAtalaOperation(
        signedWith = "master",
        operation = Some(invalidOperation),
        signature = ByteString.copyFrom(ECSignature.sign(masterKeys.getPrivate, invalidOperation.toByteArray).toArray)
      )

      val invalidBlock = atala_proto.AtalaBlock(
        operations = Seq(signedInvalidOperation)
      )

      val result = service
        .processBlock(invalidBlock)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      result mustBe false
    }
  }
}

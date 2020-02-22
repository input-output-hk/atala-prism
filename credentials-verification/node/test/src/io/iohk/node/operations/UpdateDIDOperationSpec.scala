package io.iohk.node.operations

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.models.KeyUsage
import io.iohk.node.operations.CreateDIDOperationSpec.randomProtoECKey
import io.iohk.node.repositories.{CredentialsRepository, DIDDataRepository}
import io.iohk.node.services.BlockProcessingServiceSpec
import io.iohk.node.{geud_node => proto}
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration._

object UpdateDIDOperationSpec {
  val masterKeys = CreateDIDOperationSpec.masterKeys
  val issuingKeys = CreateDIDOperationSpec.issuingKeys

  val newMasterKeys = ECKeys.generateKeyPair()

  lazy val createDidOperation = CreateDIDOperation.parse(CreateDIDOperationSpec.exampleOperation).right.value

  val exampleOperation = proto.AtalaOperation(
    operation = proto.AtalaOperation.Operation.UpdateDid(
      value = proto.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.value),
        id = createDidOperation.id.suffix,
        actions = Seq(
          proto.UpdateDIDAction(
            proto.UpdateDIDAction.Action.AddKey(
              proto.AddKeyAction(
                key = Some(
                  proto.PublicKey(
                    id = "new_master",
                    usage = proto.KeyUsage.MASTER_KEY,
                    keyData = proto.PublicKey.KeyData.EcKeyData(randomProtoECKey)
                  )
                )
              )
            )
          ),
          proto.UpdateDIDAction(
            proto.UpdateDIDAction.Action.RemoveKey(
              proto.RemoveKeyAction(
                keyId = "issuing"
              )
            )
          )
        )
      )
    )
  )
}

class UpdateDIDOperationSpec extends PostgresRepositorySpec with ProtoParsingTestHelpers {
  import UpdateDIDOperationSpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  override type Repr = UpdateDIDOperation
  override val exampleOperation = UpdateDIDOperationSpec.exampleOperation
  val signedExampleOperation = BlockProcessingServiceSpec.signOperation(exampleOperation, signingKeyId, signingKey)
  override def operationCompanion: OperationCompanion[UpdateDIDOperation] = UpdateDIDOperation

  override val tables = List("credentials", "public_keys", "did_data")

  "UpdateDIDOperation.parse" should {
    "parse valid CreateDid AtalaOperation" in {
      val result = UpdateDIDOperation.parse(signedExampleOperation).right.value
      result.actions.size mustBe exampleOperation.getUpdateDid.actions.size
    }

    "return error when id is not provided / empty" in {
      invalidValueTest(_.updateDid.id := "", Vector("updateDid", "id"), "")
    }

    "return error when AddKey id is not provided / empty" in {
      invalidValueTest(
        _.updateDid.actions(0).addKey.key.id := "",
        Vector("updateDid", "actions", "0", "addKey", "key", "id"),
        ""
      )
    }

    "return error when AddKey has invalid id" in {
      val invalidId = "it's not a valid id"
      invalidValueTest(
        _.updateDid.actions(0).addKey.key.id := invalidId,
        Vector("updateDid", "actions", "0", "addKey", "key", "id"),
        invalidId
      )
    }

    "return error when AddKey usage is not provided" in {
      invalidValueTest(
        _.updateDid.actions(0).addKey.key.usage := proto.KeyUsage.UNKNOWN_KEY,
        Vector("updateDid", "actions", "0", "addKey", "key", "usage"),
        "UNKNOWN_KEY"
      )
    }

    "return error when AddKey keyData is not provided" in {
      missingValueTest(
        _.updateDid.actions(0).addKey.key.keyData := proto.PublicKey.KeyData.Empty,
        Vector("updateDid", "actions", "0", "addKey", "key", "keyData")
      )
    }

    "return error when AddKey curve is not provided / empty" in {
      invalidValueTest(
        _.updateDid.actions(0).addKey.key.ecKeyData.curve := "",
        Vector("updateDid", "actions", "0", "addKey", "key", "ecKeyData", "curve"),
        ""
      )
    }

    "return error when AddKey key has missing x" in {
      missingValueTest(
        _.updateDid.actions(0).addKey.key.ecKeyData.x := ByteString.EMPTY,
        Vector("updateDid", "actions", "0", "addKey", "key", "ecKeyData", "x")
      )
    }

    "return error when RemoveKey id is not provided / empty" in {
      invalidValueTest(
        _.updateDid.actions(1).removeKey.keyId := "",
        Vector("updateDid", "actions", "1", "removeKey", "keyId"),
        ""
      )
    }

    "return error when RemoveKey id is invalid" in {
      val invalidId = "it's not a valid id"
      invalidValueTest(
        _.updateDid.actions(1).removeKey.keyId := invalidId,
        Vector("updateDid", "actions", "1", "removeKey", "keyId"),
        invalidId
      )
    }

    "return error when attempting to remove key used to sign" in {
      invalidValueTest(
        _.updateDid.actions(1).removeKey.keyId := "master",
        Vector("updateDid", "actions", "1", "removeKey", "keyId"),
        "master"
      )
    }

    "return error when empty action is provided" in {
      missingValueTest(
        _.updateDid.actions(1).action := proto.UpdateDIDAction.Action.Empty,
        Vector("updateDid", "actions", "1", "action")
      )
    }
  }

  "UpdateDIDOperation.getCorrectnessData" should {

    "provide the data required for correctness verification" in {
      createDidOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = UpdateDIDOperation.parse(signedExampleOperation).right.value

      val CorrectnessData(key, previousOperation) = parsedOperation
        .getCorrectnessData("master")
        .transact(database)
        .value
        .unsafeRunSync()
        .right
        .value

      key mustBe masterKeys.getPublic
      previousOperation mustBe Some(createDidOperation.digest)
    }
  }

  "UpdateDIDOperation.applyState" should {
    "update DID keys in the database" in {
      createDidOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = UpdateDIDOperation.parse(signedExampleOperation).right.value

      parsedOperation.applyState().transact(database).value.unsafeRunSync().right.value

      val did = didDataRepository.findByDidSuffix(createDidOperation.id).value.futureValue.right.value

      val initialKeys = CreateDIDOperationSpec.exampleOperation.getCreateDid.getDidData.publicKeys.map(_.id).toSet
      val expectedKeys = initialKeys + "new_master" - "issuing"
      did.keys.map(_.keyId) must contain theSameElementsAs expectedKeys

      val newKey = did.keys.find(_.keyId == "new_master").value

      newKey.keyUsage mustBe KeyUsage.MasterKey
      newKey.didSuffix mustBe createDidOperation.id
      newKey mustBe parsedOperation.actions(0).asInstanceOf[AddKeyAction].key
    }

    "return error when issuer is missing in the DB" in {
      val parsedOperation = UpdateDIDOperation.parse(signedExampleOperation).right.value

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value

      result mustBe a[StateError.EntityMissing]
    }

    "return error when removed key does not exist" in {
      createDidOperation
        .copy(keys = createDidOperation.keys.take(1))
        .applyState()
        .transact(database)
        .value
        .unsafeRunSync()

      val parsedOperation = UpdateDIDOperation.parse(signedExampleOperation).right.value

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value

      result mustBe a[StateError.EntityMissing]
    }

    "return error when added key already exists" in {
      val additionalKey = createDidOperation.keys.head.copy(keyId = "new_master")
      createDidOperation
        .copy(keys = createDidOperation.keys :+ additionalKey)
        .applyState()
        .transact(database)
        .value
        .unsafeRunSync()

      val parsedOperation = UpdateDIDOperation.parse(signedExampleOperation).right.value

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value

      result mustBe a[StateError.EntityExists]
    }

  }
}

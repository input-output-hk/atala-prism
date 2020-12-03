package io.iohk.atala.prism.node.operations

import java.time.Instant

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.node.models.{DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.randomProtoECKey
import io.iohk.atala.prism.node.repositories.{CredentialsRepository, DIDDataRepository}
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.protos.node_models
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration._

object UpdateDIDOperationSpec {
  val masterKeys = CreateDIDOperationSpec.masterKeys
  val issuingKeys = CreateDIDOperationSpec.issuingKeys

  val newMasterKeys = EC.generateKeyPair()

  lazy val dummyTimestamp = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
  lazy val createDidOperation =
    CreateDIDOperation.parse(CreateDIDOperationSpec.exampleOperation, dummyTimestamp).toOption.value

  val exampleOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.UpdateDid(
      value = node_models.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.value.toArray),
        id = createDidOperation.id.value,
        actions = Seq(
          node_models.UpdateDIDAction(
            node_models.UpdateDIDAction.Action.AddKey(
              node_models.AddKeyAction(
                key = Some(
                  node_models.PublicKey(
                    id = "new_master",
                    usage = node_models.KeyUsage.MASTER_KEY,
                    keyData = node_models.PublicKey.KeyData.EcKeyData(randomProtoECKey)
                  )
                )
              )
            )
          ),
          node_models.UpdateDIDAction(
            node_models.UpdateDIDAction.Action.RemoveKey(
              node_models.RemoveKeyAction(
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

  "UpdateDIDOperation.parse" should {
    "parse valid CreateDid AtalaOperation" in {
      val result = UpdateDIDOperation.parse(signedExampleOperation, dummyTimestamp).toOption.value
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
        _.updateDid.actions(0).addKey.key.usage := node_models.KeyUsage.UNKNOWN_KEY,
        Vector("updateDid", "actions", "0", "addKey", "key", "usage"),
        "UNKNOWN_KEY"
      )
    }

    "return error when AddKey keyData is not provided" in {
      missingValueTest(
        _.updateDid.actions(0).addKey.key.keyData := node_models.PublicKey.KeyData.Empty,
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
        _.updateDid.actions(1).action := node_models.UpdateDIDAction.Action.Empty,
        Vector("updateDid", "actions", "1", "action")
      )
    }
  }

  "UpdateDIDOperation.getCorrectnessData" should {

    "provide the data required for correctness verification" in {
      createDidOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = UpdateDIDOperation.parse(signedExampleOperation, dummyTimestamp).toOption.value

      val CorrectnessData(key, previousOperation) = parsedOperation
        .getCorrectnessData("master")
        .transact(database)
        .value
        .unsafeRunSync()
        .toOption
        .value

      key mustBe masterKeys.publicKey
      previousOperation mustBe Some(createDidOperation.digest)
    }
  }

  "UpdateDIDOperation.applyState" should {
    "update DID keys in the database" in {
      createDidOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = UpdateDIDOperation.parse(signedExampleOperation, dummyTimestamp).toOption.value

      parsedOperation.applyState().transact(database).value.unsafeRunSync().toOption.value

      val did = didDataRepository.findByDidSuffix(createDidOperation.id).value.futureValue.toOption.value

      val initialKeys = CreateDIDOperationSpec.exampleOperation.getCreateDid.getDidData.publicKeys.map(_.id).toSet
      val expectedKeys = initialKeys + "new_master" - "issuing"
      did.keys.filter(_.revokedOn.isEmpty).map(_.keyId) must contain theSameElementsAs expectedKeys

      val newKey = did.keys.find(_.keyId == "new_master").value

      newKey.keyUsage mustBe KeyUsage.MasterKey
      newKey.didSuffix mustBe createDidOperation.id
      DIDPublicKey(newKey.didSuffix, newKey.keyId, newKey.keyUsage, newKey.key) mustBe parsedOperation
        .actions(0)
        .asInstanceOf[AddKeyAction]
        .key
      newKey.addedOn mustBe dummyTimestamp
      newKey.revokedOn mustBe None
    }

    "return error when issuer is missing in the DB" in {
      val parsedOperation = UpdateDIDOperation.parse(signedExampleOperation, dummyTimestamp).toOption.value

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

      val parsedOperation = UpdateDIDOperation.parse(signedExampleOperation, dummyTimestamp).toOption.value

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

      val parsedOperation = UpdateDIDOperation.parse(signedExampleOperation, dummyTimestamp).toOption.value

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

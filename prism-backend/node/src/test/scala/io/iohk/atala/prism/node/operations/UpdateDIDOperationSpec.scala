package io.iohk.atala.prism.node.operations

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.DataPreparation.dummyLedgerData
import io.iohk.atala.prism.node.models.{DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.{randomCompressedECKeyData, randomECKeyData}
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.protos.node_models
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

object UpdateDIDOperationSpec {
  val masterKeys = CreateDIDOperationSpec.masterKeys
  val issuingKeys = CreateDIDOperationSpec.issuingKeys

  val newMasterKeys = EC.generateKeyPair()

  lazy val createDidOperation =
    CreateDIDOperation
      .parse(CreateDIDOperationSpec.exampleOperation, dummyLedgerData)
      .toOption
      .value

  val exampleAddKeyAction = node_models.UpdateDIDAction(
    node_models.UpdateDIDAction.Action.AddKey(
      node_models.AddKeyAction(
        key = Some(
          node_models.PublicKey(
            id = "new_master",
            usage = node_models.KeyUsage.MASTER_KEY,
            keyData = node_models.PublicKey.KeyData.EcKeyData(randomECKeyData)
          )
        )
      )
    )
  )

  val exampleAddKeyActionWithCompressedKeys = node_models.UpdateDIDAction(
    node_models.UpdateDIDAction.Action.AddKey(
      node_models.AddKeyAction(
        key = Some(
          node_models.PublicKey(
            id = "new_master",
            usage = node_models.KeyUsage.MASTER_KEY,
            keyData = node_models.PublicKey.KeyData.CompressedEcKeyData(
              randomCompressedECKeyData
            )
          )
        )
      )
    )
  )

  val exampleRemoveKeyAction = node_models.UpdateDIDAction(
    node_models.UpdateDIDAction.Action.RemoveKey(
      node_models.RemoveKeyAction(
        keyId = "issuing"
      )
    )
  )

  val exampleAddAndRemoveOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.UpdateDid(
      value = node_models.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue.toArray),
        id = createDidOperation.id.getValue,
        actions = Seq(exampleAddKeyAction, exampleRemoveKeyAction)
      )
    )
  )

  val exampleRemoveOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.UpdateDid(
      value = node_models.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue.toArray),
        id = createDidOperation.id.getValue,
        actions = Seq(exampleRemoveKeyAction)
      )
    )
  )

  val exampleOperationWithCompressedKeys = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.UpdateDid(
      value = node_models.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue.toArray),
        id = createDidOperation.id.getValue,
        actions = Seq(exampleAddKeyActionWithCompressedKeys, exampleRemoveKeyAction)
      )
    )
  )
}

class UpdateDIDOperationSpec extends AtalaWithPostgresSpec with ProtoParsingTestHelpers {
  import UpdateDIDOperationSpec._

  override type Repr = UpdateDIDOperation
  override val exampleOperation =
    UpdateDIDOperationSpec.exampleAddAndRemoveOperation
  val signedExampleOperation = BlockProcessingServiceSpec.signOperation(
    exampleOperation,
    signingKeyId,
    signingKey
  )
  override def operationCompanion: OperationCompanion[UpdateDIDOperation] =
    UpdateDIDOperation

  "UpdateDIDOperation.parse" should {
    "parse valid CreateDid AtalaOperation" in {
      val result = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value
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
        _.updateDid
          .actions(0)
          .addKey
          .key
          .usage := node_models.KeyUsage.UNKNOWN_KEY,
        Vector("updateDid", "actions", "0", "addKey", "key", "usage"),
        "UNKNOWN_KEY"
      )
    }

    "return error when AddKey keyData is not provided" in {
      missingValueTest(
        _.updateDid
          .actions(0)
          .addKey
          .key
          .keyData := node_models.PublicKey.KeyData.Empty,
        Vector("updateDid", "actions", "0", "addKey", "key", "keyData")
      )
    }

    "return error when AddKey curve is not provided / empty" in {
      invalidValueTest(
        _.updateDid.actions(0).addKey.key.ecKeyData.curve := "",
        Vector(
          "updateDid",
          "actions",
          "0",
          "addKey",
          "key",
          "ecKeyData",
          "curve"
        ),
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

    "return error when empty action is provided" in {
      missingValueTest(
        _.updateDid
          .actions(1)
          .action := node_models.UpdateDIDAction.Action.Empty,
        Vector("updateDid", "actions", "1", "action")
      )
    }
  }

  "UpdateDIDOperation.getCorrectnessData" should {

    "provide the data required for correctness verification" in {
      createDidOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

      val CorrectnessData(key, previousOperation) = parsedOperation
        .getCorrectnessData("master")
        .transact(database)
        .value
        .unsafeRunSync()
        .toOption
        .value

      key mustBe masterKeys.getPublicKey
      previousOperation mustBe Some(createDidOperation.digest)
    }
  }

  "UpdateDIDOperation.applyState" should {
    "update DID keys in the database" in {
      createDidOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

      parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeRunSync()
        .toOption
        .value

      val didInfo = DataPreparation.findByDidSuffix(createDidOperation.id)

      val initialKeys =
        CreateDIDOperationSpec.exampleOperation.getCreateDid.getDidData.publicKeys
          .map(_.id)
          .toSet
      val expectedKeys = initialKeys + "new_master" - "issuing"
      didInfo.keys
        .filter(_.revokedOn.isEmpty)
        .map(_.keyId) must contain theSameElementsAs expectedKeys

      val newKey = didInfo.keys.find(_.keyId == "new_master").value

      newKey.keyUsage mustBe KeyUsage.MasterKey
      newKey.didSuffix mustBe createDidOperation.id
      DIDPublicKey(
        newKey.didSuffix,
        newKey.keyId,
        newKey.keyUsage,
        newKey.key
      ) mustBe parsedOperation.actions.head
        .asInstanceOf[AddKeyAction]
        .key
      newKey.addedOn.timestampInfo mustBe dummyLedgerData.timestampInfo
      newKey.revokedOn mustBe None
      didInfo.lastOperation mustBe Sha256.compute(
        UpdateDIDOperationSpec.exampleAddAndRemoveOperation.toByteArray
      )
    }

    "return error when issuer is missing in the DB" in {
      val parsedOperation = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

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

      val parsedOperation = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

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
      val additionalKey =
        createDidOperation.keys.head.copy(keyId = "new_master")
      createDidOperation
        .copy(keys = createDidOperation.keys :+ additionalKey)
        .applyState()
        .transact(database)
        .value
        .unsafeRunSync()

      val parsedOperation = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

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

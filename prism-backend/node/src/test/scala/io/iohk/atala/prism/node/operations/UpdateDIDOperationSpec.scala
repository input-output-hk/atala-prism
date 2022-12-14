package io.iohk.atala.prism.node.operations

import cats.data.NonEmptyList
import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.DataPreparation.{dummyApplyOperationConfig, dummyLedgerData}
import io.iohk.atala.prism.node.models.{DIDPublicKey, DIDService, KeyUsage}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.{randomCompressedECKeyData, randomECKeyData}
import io.iohk.atala.prism.node.repositories.daos.{PublicKeysDAO, ServicesDAO}
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

  val exampleAddServiceAction = node_models.UpdateDIDAction(
    node_models.UpdateDIDAction.Action.AddService(
      node_models.AddServiceAction(
        Some(
          node_models.Service(
            id = "did:prism:123#linked-domain-added-via-update-did",
            `type` = "didCom-credential-exchange",
            serviceEndpoint = List(
              "https://foo.example.com",
              "https://baz.example.com"
            ),
            addedOn = None,
            deletedOn = None
          )
        )
      )
    )
  )

  val exampleRemoveServiceAction = node_models.UpdateDIDAction(
    node_models.UpdateDIDAction.Action.RemoveService(
      node_models.RemoveServiceAction(
        serviceId = "did:prism:123#linked-domain-added-via-update-did"
      )
    )
  )
  val exampleUpdateServiceAction = node_models.UpdateDIDAction(
    node_models.UpdateDIDAction.Action.UpdateService(
      node_models.UpdateServiceAction(
        serviceId = "did:prism:123#linked-domain-added-via-update-did",
        `type` = "didCom-credential-exchange-updated",
        serviceEndpoints = List(
          "https://qux.example.com"
        )
      )
    )
  )

  val exampleAddServiceOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.UpdateDid(
      value = node_models.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue.toArray),
        id = createDidOperation.id.getValue,
        actions = Seq(
          exampleAddServiceAction
        )
      )
    )
  )

  val exampleRemoveServiceOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.UpdateDid(
      value = node_models.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue.toArray),
        id = createDidOperation.id.getValue,
        actions = Seq(
          exampleRemoveServiceAction
        )
      )
    )
  )

  val exampleUpdateServiceOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.UpdateDid(
      value = node_models.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue.toArray),
        id = createDidOperation.id.getValue,
        actions = Seq(
          exampleUpdateServiceAction
        )
      )
    )
  )

  val exampleAllActionsOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.UpdateDid(
      value = node_models.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue.toArray),
        id = createDidOperation.id.getValue,
        actions = Seq(
          exampleAddKeyAction,
          exampleRemoveKeyAction,
          exampleAddServiceAction,
          exampleRemoveServiceAction,
          exampleUpdateServiceAction
        )
      )
    )
  )

  val exampleAddAndRemoveOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.UpdateDid(
      value = node_models.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue.toArray),
        id = createDidOperation.id.getValue,
        actions = Seq(
          exampleAddKeyAction,
          exampleRemoveKeyAction
        )
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
    UpdateDIDOperationSpec.exampleAllActionsOperation

  val signedExampleOperation = BlockProcessingServiceSpec.signOperation(
    exampleOperation,
    signingKeyId,
    signingKey
  )

  val signedAddAndRemoveKeysOperation = BlockProcessingServiceSpec.signOperation(
    UpdateDIDOperationSpec.exampleAddAndRemoveOperation,
    signingKeyId,
    signingKey
  )
  override def operationCompanion: OperationCompanion[UpdateDIDOperation] =
    UpdateDIDOperation

  "UpdateDIDOperation.parse" should {
    "parse valid UpdateDID AtalaOperation" in {
      val result = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value
      result.actions.size mustBe exampleOperation.getUpdateDid.actions.size
    }

    "return error when id is not provided / empty" in {
      invalidValueTest(_.updateDid.id := "", Vector("updateDid", "id"), "")
    }

    "return error when id in AddServiceAction of the service is not valid" in {
      invalidValueTest(
        _.updateDid
          .actions(2)
          .addService
          .service
          .modify(_.copy(id = "not valid URI")),
        Vector("updateDid", "actions", "2", "addService", "service", "id"),
        "not valid URI"
      )
    }

    "return error when one of the service endpoints in AddServiceAction of the service is not valid" in {
      invalidValueTest(
        _.updateDid
          .actions(2)
          .addService
          .service
          .modify(_.copy(serviceEndpoint = List("https://foo.example.com", "not valid URI"))),
        Vector("updateDid", "actions", "2", "addService", "service", "serviceEndpoint", "1"),
        "not valid URI"
      )
    }

    "return error when type in AddServiceAction of the service is empty" in {
      missingValueTest(
        _.updateDid
          .actions(2)
          .addService
          .service
          .modify(_.copy(`type` = "")),
        Vector("updateDid", "actions", "2", "addService", "service", "type")
      )
    }

    "return error when service endpoints in AddServiceAction of the service is empty" in {
      invalidValueTest(
        _.updateDid
          .actions(2)
          .addService
          .service
          .modify(_.copy(serviceEndpoint = Nil)),
        Vector("updateDid", "actions", "2", "addService", "service", "serviceEndpoint"),
        "List()"
      )
    }

    "return error when id in RemoveServiceAction is not valid" in {
      invalidValueTest(
        _.updateDid.actions(3).removeService.serviceId := "not valid URI",
        Vector("updateDid", "actions", "3", "removeService", "serviceId"),
        "not valid URI"
      )
    }

    "return error if id of the service in UpdateService is not valid" in {
      invalidValueTest(
        _.updateDid
          .actions(4)
          .updateService
          .serviceId := "not valid URI",
        Vector("updateDid", "actions", "4", "updateService", "serviceId"),
        "not valid URI"
      )
    }

    "return error if both type and service endpoints are empty in UpdateService" in {
      missingAtLeastOneValueTest(
        _.updateDid
          .actions(4)
          .updateService
          .modify(_.copy(`type` = "", serviceEndpoints = Nil)),
        NonEmptyList(
          Vector("updateDid", "actions", "4", "updateService", "type"),
          List(Vector("updateDid", "actions", "4", "updateService", "serviceEndpoints"))
        )
      )
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
      createDidOperation.applyState(dummyApplyOperationConfig).transact(database).value.unsafeRunSync()

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

    "fail given master key revoked" in {
      createDidOperation.applyState(dummyApplyOperationConfig).transact(database).value.unsafeRunSync()

      PublicKeysDAO
        .revoke(createDidOperation.id, "master", dummyLedgerData)
        .transact(database)
        .unsafeRunSync()

      val parsedOperation = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .getCorrectnessData("master")
        .transact(database)
        .value
        .unsafeRunSync()

      result mustBe Left(StateError.KeyAlreadyRevoked())
    }
  }

  "UpdateDIDOperation.applyState" should {
    "update DID keys in the database" in {
      createDidOperation.applyState(dummyApplyOperationConfig).transact(database).value.unsafeRunSync()

      val parsedOperation = UpdateDIDOperation
        .parse(signedAddAndRemoveKeysOperation, dummyLedgerData)
        .toOption
        .value

      parsedOperation
        .applyState(dummyApplyOperationConfig)
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

    "return error when DID is missing in the DB" in {
      val parsedOperation = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)
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
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      val parsedOperation = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)
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
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      val parsedOperation = UpdateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value

      result mustBe a[StateError.EntityExists]
    }

    "return error when the last master key is revoked" in {
      createDidOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      val revokeTheMaster = node_models.AtalaOperation(
        operation = node_models.AtalaOperation.Operation.UpdateDid(
          value = node_models.UpdateDIDOperation(
            previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue),
            id = createDidOperation.id.getValue,
            actions = Seq(
              node_models.UpdateDIDAction(
                node_models.UpdateDIDAction.Action.RemoveKey(
                  node_models.RemoveKeyAction(
                    keyId = "master"
                  )
                )
              )
            )
          )
        )
      )

      val parsedOperation = UpdateDIDOperation
        .parse(revokeTheMaster, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value

      result mustBe a[StateError.InvalidMasterKeyRevocation]
    }

    "update DID when master key is being replaced" in {
      createDidOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      val replaceTheMaster = node_models.AtalaOperation(
        operation = node_models.AtalaOperation.Operation.UpdateDid(
          value = node_models.UpdateDIDOperation(
            previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue),
            id = createDidOperation.id.getValue,
            actions = Seq(
              exampleAddKeyAction,
              node_models.UpdateDIDAction(
                node_models.UpdateDIDAction.Action.RemoveKey(
                  node_models.RemoveKeyAction(
                    keyId = "master"
                  )
                )
              )
            )
          )
        )
      )

      val parsedOperation = UpdateDIDOperation
        .parse(replaceTheMaster, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

      result mustBe Right(())
    }

    "Add service on updateDID when AddService action is used" in {
      createDidOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      val parsedOperation = UpdateDIDOperation
        .parse(exampleAddServiceOperation, dummyLedgerData)
        .toOption
        .value

      parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

      val service = ServicesDAO
        .get(createDidOperation.id, "did:prism:123#linked-domain-added-via-update-did")
        .transact(database)
        .unsafeRunSync()

      service.nonEmpty mustBe true

    }

    "Remove service on updateDID when RemoveService action is used" in {
      createDidOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      ServicesDAO
        .insert(
          DIDService(
            id = "did:prism:123#linked-domain-added-via-update-did",
            didSuffix = createDidOperation.id,
            `type` = "to-be-revoked",
            serviceEndpoints = Nil
          ),
          dummyLedgerData
        )
        .transact(database)
        .unsafeRunSync()

      val serviceBeforeRevocation = ServicesDAO
        .get(createDidOperation.id, "did:prism:123#linked-domain-added-via-update-did")
        .transact(database)
        .unsafeRunSync()

      serviceBeforeRevocation.nonEmpty mustBe true

      val parsedOperation = UpdateDIDOperation
        .parse(exampleRemoveServiceOperation, dummyLedgerData)
        .toOption
        .value

      parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

      val serviceAfterRevocation = ServicesDAO
        .get(createDidOperation.id, "did:prism:123#linked-domain-added-via-update-did")
        .transact(database)
        .unsafeRunSync()

      serviceAfterRevocation.nonEmpty mustBe false

    }

    "update service on updateDID when UpdateServiceAction is used" in {
      createDidOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      ServicesDAO
        .insert(
          DIDService(
            id = "did:prism:123#linked-domain-added-via-update-did",
            didSuffix = createDidOperation.id,
            `type` = "to-be-updated",
            serviceEndpoints = Nil
          ),
          dummyLedgerData
        )
        .transact(database)
        .unsafeRunSync()

      val serviceBeforeUpdate = ServicesDAO
        .get(createDidOperation.id, "did:prism:123#linked-domain-added-via-update-did")
        .transact(database)
        .unsafeRunSync()

      serviceBeforeUpdate.nonEmpty mustBe true
      serviceBeforeUpdate.value.`type` mustBe "to-be-updated"
      serviceBeforeUpdate.value.serviceEndpoints.size mustBe 0

      val parsedOperation = UpdateDIDOperation
        .parse(exampleUpdateServiceOperation, dummyLedgerData)
        .toOption
        .value

      parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

      val serviceAfterUpdate = ServicesDAO
        .get(createDidOperation.id, "did:prism:123#linked-domain-added-via-update-did")
        .transact(database)
        .unsafeRunSync()

      serviceAfterUpdate.nonEmpty mustBe true
      serviceAfterUpdate.value.`type` mustBe "didCom-credential-exchange-updated"
      serviceAfterUpdate.value.serviceEndpoints.size mustBe 1
      serviceAfterUpdate.value.serviceEndpoints.head.url mustBe "https://qux.example.com"
    }

  }
}

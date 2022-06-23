package io.iohk.atala.prism.node.operations

import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.DataPreparation.{dummyApplyOperationConfig, dummyLedgerData}
import io.iohk.atala.prism.node.repositories.daos.PublicKeysDAO
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{AtalaOperation, SignedAtalaOperation}
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

object DeactivateDIDOperationSpec {
  val masterKeys = CreateDIDOperationSpec.masterKeys
  val issuingKeys = CreateDIDOperationSpec.issuingKeys

  // val newMasterKeys = EC.generateKeyPair()

  val createDidOperation: CreateDIDOperation =
    CreateDIDOperation
      .parse(CreateDIDOperationSpec.exampleOperation, dummyLedgerData)
      .toOption
      .value

  val exampleOperation: AtalaOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.DeactivateDid(
      value = node_models.DeactivateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.getValue),
        id = createDidOperation.id.getValue
      )
    )
  )
}

class DeactivateDIDOperationSpec extends AtalaWithPostgresSpec with ProtoParsingTestHelpers {

  override type Repr = DeactivateDIDOperation
  override val exampleOperation: AtalaOperation = DeactivateDIDOperationSpec.exampleOperation

  val signedExampleOperation: SignedAtalaOperation = BlockProcessingServiceSpec.signOperation(
    exampleOperation,
    signingKeyId,
    signingKey
  )

  override def operationCompanion: OperationCompanion[DeactivateDIDOperation] =
    DeactivateDIDOperation

  "DeactivateDIDOperation.parse" should {
    "parse valid DeactivateDID AtalaOperation" in {
      val result = DeactivateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value
      result.didSuffix mustBe DidSuffix(exampleOperation.getDeactivateDid.id)
      result.previousOperation mustBe Sha256Digest.fromBytes(
        exampleOperation.getDeactivateDid.previousOperationHash.toByteArray
      )
    }

    "return error when id is not provided / empty" in {
      invalidValueTest(_.deactivateDid.id := "", Vector("deactivateDid", "id"), "")
    }
  }

  "DeactivateDIDOperation.getCorrectnessData" should {

    "provide the data required for correctness verification" in {
      DeactivateDIDOperationSpec.createDidOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      val parsedOperation = DeactivateDIDOperation
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

      key mustBe DeactivateDIDOperationSpec.masterKeys.getPublicKey
      previousOperation mustBe Some(DeactivateDIDOperationSpec.createDidOperation.digest)
    }

    "fail given master key revoked" in {
      DeactivateDIDOperationSpec.createDidOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      PublicKeysDAO
        .revoke(DeactivateDIDOperationSpec.createDidOperation.id, "master", dummyLedgerData)
        .transact(database)
        .unsafeRunSync()

      val parsedOperation = DeactivateDIDOperation
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

  "DeactivateDIDOperation.applyState" should {
    "deactivate DID and revoke all keys in the database" in {
      DeactivateDIDOperationSpec.createDidOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      val parsedOperation = DeactivateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

      parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()
        .toOption
        .value

      val didInfo = DataPreparation.findByDidSuffix(DeactivateDIDOperationSpec.createDidOperation.id)

      didInfo.keys.filter(_.revokedOn.isEmpty) mustBe List()
    }

    "return error when DID is missing in the DB" in {
      val parsedOperation = DeactivateDIDOperation
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
  }

}

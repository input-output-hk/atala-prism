package io.iohk.atala.prism.node.operations

import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.node.models.DidSuffix
import io.iohk.atala.prism.node.{AtalaWithPostgresSpec, DataPreparation}
import io.iohk.atala.prism.node.DataPreparation.{dummyApplyOperationConfig, dummyLedgerData}
import io.iohk.atala.prism.node.crypto.CryptoTestUtils.SecpPair
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.repositories.daos.PublicKeysDAO
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{AtalaOperation, SignedAtalaOperation}
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

object DeactivateDIDOperationSpec {
  val masterKeys: SecpPair = CreateDIDOperationSpec.masterKeys
  val issuingKeys: SecpPair = CreateDIDOperationSpec.issuingKeys

  val createDidOperation: CreateDIDOperation =
    CreateDIDOperation
      .parse(CreateDIDOperationSpec.exampleOperation, dummyLedgerData)
      .toOption
      .value

  val exampleOperation: AtalaOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.DeactivateDid(
      value = node_models.DeactivateDIDOperation(
        previousOperationHash = ByteString.copyFrom(createDidOperation.digest.bytes.toArray),
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
      result.previousOperation mustBe Sha256Hash.fromBytes(
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

      key.compressed.toVector mustBe DeactivateDIDOperationSpec.masterKeys.publicKey.compressed.toVector
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
    "deactivate DID, revoke all keys and services in the database" in {
      DeactivateDIDOperationSpec.createDidOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()

      val parsedOperation = DeactivateDIDOperation
        .parse(signedExampleOperation, dummyLedgerData)
        .toOption
        .value

      val didInfoBeforeRevocation = DataPreparation.findByDidSuffix(DeactivateDIDOperationSpec.createDidOperation.id)

      didInfoBeforeRevocation.services.size mustBe 2

      parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeRunSync()
        .toOption
        .value

      val didInfoAfterRevocation = DataPreparation.findByDidSuffix(DeactivateDIDOperationSpec.createDidOperation.id)

      didInfoAfterRevocation.keys.filter(_.revokedOn.isEmpty) mustBe List()
      didInfoAfterRevocation.services.size mustBe 0
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

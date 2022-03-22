package io.iohk.atala.prism.node.operations

import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.node.DataPreparation.{dummyApplyOperationConfig, dummyLedgerData, dummyTimestampInfo}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.nodeState.DIDPublicKeyState
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey}
import io.iohk.atala.prism.node.operations.StateError.UnsupportedOperation
import io.iohk.atala.prism.node.operations.protocolVersion.SupportedOperations
import io.iohk.atala.prism.node.{DataPreparation, models}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{CompressedECKeyData, ECKeyData}
import org.scalatest.EitherValues._
import org.scalatest.Inside._
import org.scalatest.OptionValues._

object CreateDIDOperationSpec {
  def protoECKeyDataFromPublicKey(key: ECPublicKey): ECKeyData = {
    val point = key.getCurvePoint

    node_models.ECKeyData(
      curve = ECConfig.getCURVE_NAME,
      x = ByteString.copyFrom(point.getX.bytes()),
      y = ByteString.copyFrom(point.getY.bytes())
    )
  }

  def protoCompressedECKeyDataFromPublicKey(
      key: ECPublicKey
  ): CompressedECKeyData =
    node_models.CompressedECKeyData(
      curve = ECConfig.getCURVE_NAME,
      data = ByteString.copyFrom(key.getEncodedCompressed)
    )

  def randomECKeyData: ECKeyData = {
    val keyPair = EC.generateKeyPair()
    protoECKeyDataFromPublicKey(keyPair.getPublicKey)
  }

  def randomCompressedECKeyData: CompressedECKeyData = {
    val keyPair = EC.generateKeyPair()
    protoCompressedECKeyDataFromPublicKey(keyPair.getPublicKey)
  }

  val masterKeys: ECKeyPair = EC.generateKeyPair()
  val masterEcKeyData: ECKeyData = protoECKeyDataFromPublicKey(
    masterKeys.getPublicKey
  )
  val masterCompressedEcKeyData: CompressedECKeyData =
    protoCompressedECKeyDataFromPublicKey(masterKeys.getPublicKey)

  val issuingKeys: ECKeyPair = EC.generateKeyPair()
  val issuingEcKeyData: ECKeyData = protoECKeyDataFromPublicKey(
    issuingKeys.getPublicKey
  )
  val issuingCompressedEcKeyData: CompressedECKeyData =
    protoCompressedECKeyDataFromPublicKey(issuingKeys.getPublicKey)

  val revokingKeys: ECKeyPair = EC.generateKeyPair()
  val revokingEcKeyData: ECKeyData = protoECKeyDataFromPublicKey(
    revokingKeys.getPublicKey
  )
  val revokingCompressedEcKeyData: CompressedECKeyData =
    protoCompressedECKeyDataFromPublicKey(
      revokingKeys.getPublicKey
    )

  val exampleOperation: node_models.AtalaOperation = node_models.AtalaOperation(
    node_models.AtalaOperation.Operation.CreateDid(
      value = node_models.CreateDIDOperation(
        didData = Some(
          node_models.CreateDIDOperation.DIDCreationData(
            publicKeys = List(
              node_models.PublicKey(
                "master",
                node_models.KeyUsage.MASTER_KEY,
                Some(
                  node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                ),
                None,
                node_models.PublicKey.KeyData.EcKeyData(masterEcKeyData)
              ),
              node_models
                .PublicKey(
                  "issuing",
                  node_models.KeyUsage.ISSUING_KEY,
                  Some(
                    node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                  ),
                  None,
                  node_models.PublicKey.KeyData.EcKeyData(issuingEcKeyData)
                ),
              node_models
                .PublicKey(
                  "revoking",
                  node_models.KeyUsage.REVOCATION_KEY,
                  Some(
                    node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                  ),
                  None,
                  node_models.PublicKey.KeyData.EcKeyData(revokingEcKeyData)
                ),
              node_models.PublicKey(
                "authentication",
                node_models.KeyUsage.AUTHENTICATION_KEY,
                Some(
                  node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                ),
                None,
                node_models.PublicKey.KeyData.EcKeyData(randomECKeyData)
              )
            )
          )
        )
      )
    )
  )

  val exampleOperationWithCompressedKeys: node_models.AtalaOperation =
    node_models.AtalaOperation(
      node_models.AtalaOperation.Operation.CreateDid(
        value = node_models.CreateDIDOperation(
          didData = Some(
            node_models.CreateDIDOperation.DIDCreationData(
              publicKeys = List(
                node_models.PublicKey(
                  "master",
                  node_models.KeyUsage.MASTER_KEY,
                  Some(
                    node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                  ),
                  None,
                  node_models.PublicKey.KeyData.EcKeyData(masterEcKeyData)
                ),
                node_models
                  .PublicKey(
                    "issuing",
                    node_models.KeyUsage.ISSUING_KEY,
                    Some(
                      node_models.LedgerData(timestampInfo =
                        Some(
                          ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)
                        )
                      )
                    ),
                    None,
                    node_models.PublicKey.KeyData
                      .CompressedEcKeyData(issuingCompressedEcKeyData)
                  ),
                node_models
                  .PublicKey(
                    "revoking",
                    node_models.KeyUsage.REVOCATION_KEY,
                    Some(
                      node_models.LedgerData(timestampInfo =
                        Some(
                          ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)
                        )
                      )
                    ),
                    None,
                    node_models.PublicKey.KeyData
                      .CompressedEcKeyData(revokingCompressedEcKeyData)
                  ),
                node_models.PublicKey(
                  "authentication",
                  node_models.KeyUsage.AUTHENTICATION_KEY,
                  Some(
                    node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                  ),
                  None,
                  node_models.PublicKey.KeyData.CompressedEcKeyData(
                    randomCompressedECKeyData
                  )
                )
              )
            )
          )
        )
      )
    )

}

class CreateDIDOperationSpec extends AtalaWithPostgresSpec {

  import CreateDIDOperationSpec._

  "CreateDIDOperation.parse" should {
    "parse valid CreateDid AtalaOperation" in {
      CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData) mustBe a[Right[_, _]]
    }

    "return error when a key has missing curve name" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).ecKeyData.curve := "")

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "ecKeyData",
            "curve"
          )
      }
    }

    "return error when a key has missing x" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData.publicKeys(0).ecKeyData.x := ByteString.EMPTY
        )

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.MissingValue(path)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "ecKeyData",
            "x"
          )
      }
    }

    "return error when a key has missing data" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData
            .publicKeys(0)
            .keyData := node_models.PublicKey.KeyData.Empty
        )

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.MissingValue(path)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "keyData"
          )
      }
    }

    "return error when a key has missing id" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).id := "")

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "id"
          )
      }
    }

    "return error when a key has invalid id" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData.publicKeys(0).id := "it isn't proper key id"
        )

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "id"
          )
      }
    }

    "return error when a key has invalid usage" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData
            .publicKeys(0)
            .usage := node_models.KeyUsage.UNKNOWN_KEY
        )

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "usage"
          )
      }
    }

    "return error when master key doesn't exist" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData
            .publicKeys(0)
            .usage := node_models.KeyUsage.ISSUING_KEY
        )

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys"
          )
      }
    }
  }

  "CreateDIDOperation.getCorrectnessData" should {
    "provide the key to be used for signing" in {
      val parsedOperation = CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData)
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
      previousOperation mustBe None
    }
  }

  private def toDIDPublicKey(keyState: DIDPublicKeyState): DIDPublicKey = {
    DIDPublicKey(
      didSuffix = keyState.didSuffix,
      keyId = keyState.keyId,
      keyUsage = keyState.keyUsage,
      key = keyState.key
    )
  }

  "CreateDIDOperation.applyState" should {

    "create the DID information in the database" in {
      val parsedOperation = CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
      result mustBe a[Right[_, _]]

      val didState = DataPreparation.findByDidSuffix(parsedOperation.id)

      didState.didSuffix mustBe parsedOperation.id
      didState.lastOperation mustBe parsedOperation.digest
      didState.keys.map(
        toDIDPublicKey
      ) must contain theSameElementsAs parsedOperation.keys

      for (key <- parsedOperation.keys) {
        val keyState =
          DataPreparation.findKey(parsedOperation.id, key.keyId).value
        DIDPublicKey(
          keyState.didSuffix,
          keyState.keyId,
          keyState.keyUsage,
          keyState.key
        ) mustBe key
        keyState.addedOn.timestampInfo mustBe dummyLedgerData.timestampInfo
        keyState.revokedOn mustBe None
      }
    }

    "return error when given DID already exists" in {
      val parsedOperation = CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

      DataPreparation
        .createDID(
          DIDData(parsedOperation.id, Nil, parsedOperation.digest),
          dummyLedgerData
        )

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

      inside(result.left.value) { case StateError.EntityExists(tpe, _) =>
        tpe mustBe "DID"
      }
    }

    "return error when CreateDID operation is not supported by the current protocol" in {
      // Let's pretend that none of operations are supported
      val fakeSupportedOperations: SupportedOperations =
        (_: Operation, _: models.ProtocolVersion) => false
      val parsedOperation = CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)(fakeSupportedOperations)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value
      result mustBe a[UnsupportedOperation]
    }
  }
}

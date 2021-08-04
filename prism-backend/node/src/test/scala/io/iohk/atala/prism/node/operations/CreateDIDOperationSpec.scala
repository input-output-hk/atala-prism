package io.iohk.atala.prism.node.operations

import cats.effect.IO
import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.kotlin.credentials.TimestampInfo
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey}
import io.iohk.atala.prism.node.repositories.DIDDataRepository
import io.iohk.atala.prism.protos.node_models
import org.scalatest.EitherValues._
import org.scalatest.Inside._
import org.scalatest.OptionValues._

import java.time.Instant
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.protos.node_models.ECKeyData

object CreateDIDOperationSpec {
  def protoECKeyFromPublicKey(key: ECPublicKey): ECKeyData = {
    val point = key.getCurvePoint

    node_models.ECKeyData(
      curve = ECConfig.getCURVE_NAME,
      x = ByteString.copyFrom(point.getX.bytes()),
      y = ByteString.copyFrom(point.getY.bytes())
    )
  }

  def randomProtoECKey: ECKeyData = {
    val keyPair = EC.generateKeyPair()
    protoECKeyFromPublicKey(keyPair.getPublicKey)
  }

  val masterKeys: ECKeyPair = EC.generateKeyPair()
  val masterEcKey: ECKeyData = protoECKeyFromPublicKey(masterKeys.getPublicKey)

  val issuingKeys: ECKeyPair = EC.generateKeyPair()
  val issuingEcKey: ECKeyData = protoECKeyFromPublicKey(issuingKeys.getPublicKey)

  val revokingKeys: ECKeyPair = EC.generateKeyPair()
  val revokingEcKey: ECKeyData = protoECKeyFromPublicKey(revokingKeys.getPublicKey)

  lazy val dummyTimestamp: TimestampInfo = dummyTimestampInfo
  lazy val dummyLedgerData: LedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    dummyTimestamp
  )

  private val dummyTimestampInfo = new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)

  val exampleOperation: node_models.AtalaOperation = node_models.AtalaOperation(
    node_models.AtalaOperation.Operation.CreateDid(
      value = node_models.CreateDIDOperation(
        didData = Some(
          node_models.DIDData(
            id = "",
            publicKeys = List(
              node_models.PublicKey(
                "master",
                node_models.KeyUsage.MASTER_KEY,
                Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)),
                None,
                node_models.PublicKey.KeyData.EcKeyData(masterEcKey)
              ),
              node_models
                .PublicKey(
                  "issuing",
                  node_models.KeyUsage.ISSUING_KEY,
                  Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)),
                  None,
                  node_models.PublicKey.KeyData.EcKeyData(issuingEcKey)
                ),
              node_models
                .PublicKey(
                  "revoking",
                  node_models.KeyUsage.REVOCATION_KEY,
                  Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)),
                  None,
                  node_models.PublicKey.KeyData.EcKeyData(revokingEcKey)
                ),
              node_models.PublicKey(
                "authentication",
                node_models.KeyUsage.AUTHENTICATION_KEY,
                Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)),
                None,
                node_models.PublicKey.KeyData.EcKeyData(randomProtoECKey)
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

  lazy val didDataRepository: DIDDataRepository[IO] = DIDDataRepository(database)
  "CreateDIDOperation.parse" should {
    "parse valid CreateDid AtalaOperation" in {
      CreateDIDOperation.parse(exampleOperation, dummyLedgerData) mustBe a[Right[_, _]]
    }

    "return error when id is provided" in {
      val providedDidSuffix = Array.fill(32)("00").mkString("")
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.id := providedDidSuffix)

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("createDid", "didData", "id")
          value mustBe providedDidSuffix
      }
    }

    "return error when a key has missing curve name" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).ecKeyData.curve := "")

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "ecKeyData", "curve")
      }
    }

    "return error when a key has missing x" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).ecKeyData.x := ByteString.EMPTY)

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.MissingValue(path)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "ecKeyData", "x")
      }
    }

    "return error when a key has missing data" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).keyData := node_models.PublicKey.KeyData.Empty)

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.MissingValue(path)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "keyData")
      }
    }

    "return error when a key has missing id" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).id := "")

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "id")
      }
    }

    "return error when a key has invalid id" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).id := "it isn't proper key id")

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "id")
      }
    }

    "return error when a key has invalid usage" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).usage := node_models.KeyUsage.UNKNOWN_KEY)

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "usage")
      }
    }
  }

  "CreateDIDOperation.getCorrectnessData" should {
    "provide the key to be used for signing" in {
      val parsedOperation = CreateDIDOperation.parse(exampleOperation, dummyLedgerData).toOption.value
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
      val parsedOperation = CreateDIDOperation.parse(exampleOperation, dummyLedgerData).toOption.value

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
      result mustBe a[Right[_, _]]

      val didState = DataPreparation.findByDidSuffix(parsedOperation.id)

      didState.didSuffix mustBe parsedOperation.id
      didState.lastOperation mustBe parsedOperation.digest
      didState.keys.map(toDIDPublicKey) must contain theSameElementsAs parsedOperation.keys

      for (key <- parsedOperation.keys) {
        val keyState = DataPreparation.findKey(parsedOperation.id, key.keyId).value
        DIDPublicKey(keyState.didSuffix, keyState.keyId, keyState.keyUsage, keyState.key) mustBe key
        keyState.addedOn mustBe dummyLedgerData.timestampInfo
        keyState.revokedOn mustBe None
      }
    }

    "return error when given DID already exists" in {
      val parsedOperation = CreateDIDOperation.parse(exampleOperation, dummyLedgerData).toOption.value

      DataPreparation
        .createDID(DIDData(parsedOperation.id, Nil, parsedOperation.digest), dummyLedgerData)

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

      inside(result.left.value) {
        case StateError.EntityExists(tpe, _) =>
          tpe mustBe "DID"
      }

    }
  }

}

package io.iohk.node.operations

import java.security.PublicKey

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.grpc.ProtoCodecs
import io.iohk.node.models.{DIDData, DIDPublicKey}
import io.iohk.node.repositories.DIDDataRepository
import io.iohk.prism.protos.node_models
import org.scalatest.EitherValues._
import org.scalatest.Inside._

import scala.concurrent.duration._

object CreateDIDOperationSpec {
  def protoECKeyFromPublicKey(key: PublicKey) = {
    val point = ECKeys.getECPoint(key)

    node_models.ECKeyData(
      curve = ECKeys.CURVE_NAME,
      x = ByteString.copyFrom(point.getAffineX.toByteArray),
      y = ByteString.copyFrom(point.getAffineY.toByteArray)
    )
  }

  def randomProtoECKey = {
    val keyPair = ECKeys.generateKeyPair()
    protoECKeyFromPublicKey(keyPair.getPublic)
  }

  val masterKeys = ECKeys.generateKeyPair()
  val masterEcKey = protoECKeyFromPublicKey(masterKeys.getPublic)

  val issuingKeys = ECKeys.generateKeyPair()
  val issuingEcKey = protoECKeyFromPublicKey(issuingKeys.getPublic)

  val exampleOperation = node_models.AtalaOperation(
    node_models.AtalaOperation.Operation.CreateDid(
      value = node_models.CreateDIDOperation(
        didData = Some(
          node_models.DIDData(
            id = "",
            publicKeys = List(
              node_models.PublicKey(
                "master",
                node_models.KeyUsage.MASTER_KEY,
                Some(ProtoCodecs.toTimeStampInfoProto(TimestampInfo.dummyTime)),
                None,
                node_models.PublicKey.KeyData.EcKeyData(masterEcKey)
              ),
              node_models
                .PublicKey(
                  "issuing",
                  node_models.KeyUsage.ISSUING_KEY,
                  Some(ProtoCodecs.toTimeStampInfoProto(TimestampInfo.dummyTime)),
                  None,
                  node_models.PublicKey.KeyData.EcKeyData(issuingEcKey)
                ),
              node_models.PublicKey(
                "authentication",
                node_models.KeyUsage.AUTHENTICATION_KEY,
                Some(ProtoCodecs.toTimeStampInfoProto(TimestampInfo.dummyTime)),
                None,
                node_models.PublicKey.KeyData.EcKeyData(randomProtoECKey)
              ),
              node_models.PublicKey(
                "communication",
                node_models.KeyUsage.COMMUNICATION_KEY,
                Some(ProtoCodecs.toTimeStampInfoProto(TimestampInfo.dummyTime)),
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

class CreateDIDOperationSpec extends PostgresRepositorySpec {

  import CreateDIDOperationSpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val dummyTimestamp = TimestampInfo.dummyTime

  "CreateDIDOperation.parse" should {
    "parse valid CreateDid AtalaOperation" in {
      CreateDIDOperation.parse(exampleOperation, dummyTimestamp) mustBe a[Right[_, _]]
    }

    "return error when id is provided" in {
      val providedDidSuffix = Array.fill(32)("00").mkString("")
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.id := providedDidSuffix)

      inside(CreateDIDOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("createDid", "didData", "id")
          value mustBe providedDidSuffix
      }
    }

    "return error when a key has missing curve name" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).ecKeyData.curve := "")

      inside(CreateDIDOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "ecKeyData", "curve")
      }
    }

    "return error when a key has missing x" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).ecKeyData.x := ByteString.EMPTY)

      inside(CreateDIDOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.MissingValue(path)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "ecKeyData", "x")
      }
    }

    "return error when a key has missing data" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).keyData := node_models.PublicKey.KeyData.Empty)

      inside(CreateDIDOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.MissingValue(path)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "keyData")
      }
    }

    "return error when a key has missing id" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).id := "")

      inside(CreateDIDOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "id")
      }
    }

    "return error when a key has invalid id" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).id := "it isn't proper key id")

      inside(CreateDIDOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "id")
      }
    }

    "return error when a key has invalid usage" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).usage := node_models.KeyUsage.UNKNOWN_KEY)

      inside(CreateDIDOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "usage")
      }
    }
  }

  "CreateDIDOperation.getCorrectnessData" should {
    "provide the key to be used for signing" in {
      val parsedOperation = CreateDIDOperation.parse(exampleOperation, dummyTimestamp).right.value
      val CorrectnessData(key, previousOperation) = parsedOperation
        .getCorrectnessData("master")
        .transact(database)
        .value
        .unsafeRunSync()
        .right
        .value

      key mustBe masterKeys.getPublic
      previousOperation mustBe None
    }
  }

  "CreateDIDOperation.applyState" should {
    "create the DID information in the database" in {
      val parsedOperation = CreateDIDOperation.parse(exampleOperation, dummyTimestamp).right.value

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
      result mustBe a[Right[_, _]]

      didDataRepository.findByDidSuffix(parsedOperation.id).value.futureValue mustBe a[Right[_, _]]

      for (key <- parsedOperation.keys) {
        val keyState = didDataRepository.findKey(parsedOperation.id, key.keyId).value.futureValue.right.value
        DIDPublicKey(keyState.didSuffix, keyState.keyId, keyState.keyUsage, keyState.key) mustBe key
        keyState.addedOn mustBe dummyTimestamp
        keyState.revokedOn mustBe None
      }
    }

    "return error when given DID already exists" in {
      val parsedOperation = CreateDIDOperation.parse(exampleOperation, dummyTimestamp).right.value

      didDataRepository
        .create(DIDData(parsedOperation.id, Nil, parsedOperation.digest), dummyTimestamp)
        .value
        .futureValue

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

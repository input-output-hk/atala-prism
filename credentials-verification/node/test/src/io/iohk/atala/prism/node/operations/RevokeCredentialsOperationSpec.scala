package io.iohk.atala.prism.node.operations

import java.time.Instant

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.{DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.repositories.DIDDataRepository
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.scalatest.EitherValues._
import org.scalatest.Inside._
import org.scalatest.OptionValues.convertOptionToValuable

import scala.concurrent.duration._

object RevokeCredentialsOperationSpec {
  val masterKeys = CreateDIDOperationSpec.masterKeys
  val issuingKeys = CreateDIDOperationSpec.issuingKeys

  lazy val issuerDidKeys = List(
    DIDPublicKey(issuerDIDSuffix, "master", KeyUsage.MasterKey, masterKeys.publicKey),
    DIDPublicKey(issuerDIDSuffix, "issuing", KeyUsage.IssuingKey, issuingKeys.publicKey)
  )

  lazy val dummyTimestamp = TimestampInfo.dummyTime
  lazy val issuerCreateDIDOperation =
    CreateDIDOperation.parse(CreateDIDOperationSpec.exampleOperation, dummyTimestamp).right.value
  lazy val credentialIssueBatchOperation =
    IssueCredentialBatchOperation.parse(IssueCredentialBatchOperationSpec.exampleOperation, dummyTimestamp).right.value

  lazy val issuerDIDSuffix = issuerCreateDIDOperation.id
  lazy val credentialBatchId = credentialIssueBatchOperation.credentialBatchId

  val revocationDate = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)

  val revokeFullBatchOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.RevokeCredentials(
      value = node_models.RevokeCredentialsOperation(
        previousOperationHash = ByteString.copyFrom(credentialIssueBatchOperation.digest.value.toArray),
        credentialBatchId = credentialBatchId.id,
        credentialsToRevoke = Seq()
      )
    )
  )

  val credentialHashToRevoke = SHA256Digest.compute("cred 1".getBytes)
  val credentialHashNotRevoked = SHA256Digest.compute("cred 2".getBytes)

  val revokeSpecificCredentialsOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.RevokeCredentials(
      value = node_models.RevokeCredentialsOperation(
        previousOperationHash = ByteString.copyFrom(credentialIssueBatchOperation.digest.value.toArray),
        credentialBatchId = credentialBatchId.id,
        credentialsToRevoke = Seq(ByteString.copyFrom(credentialHashToRevoke.value.toArray))
      )
    )
  )
}

class RevokeCredentialsOperationSpec extends PostgresRepositorySpec {

  import RevokeCredentialsOperationSpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)

  "RevokeCredentialsOperation.parse" should {
    "parse valid RevokeCredentials AtalaOperation to revoke a full batch" in {
      RevokeCredentialsOperation.parse(revokeFullBatchOperation, dummyTimestamp) mustBe a[Right[_, _]]
    }

    "parse valid RevokeCredentials AtalaOperation to revoke specific credentials within a batch" in {
      RevokeCredentialsOperation.parse(revokeSpecificCredentialsOperation, dummyTimestamp) mustBe a[Right[_, _]]
    }

    "return error when no previous operation is provided" in {
      val invalidOperation = revokeFullBatchOperation
        .update(_.revokeCredentials.previousOperationHash := ByteString.EMPTY)

      inside(RevokeCredentialsOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredentials", "previousOperationHash")
          value mustBe "0x0"
      }
    }

    "return error when previous operation hash has invalid length" in {
      val bs = ByteString.copyFromUtf8("abc")
      val invalidOperation = revokeFullBatchOperation
        .update(_.revokeCredentials.previousOperationHash := bs)

      inside(RevokeCredentialsOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredentials", "previousOperationHash")
          value mustBe "0x616263"
      }
    }

    "return error if no credential batch id is provided" in {
      val invalidOperation = revokeFullBatchOperation
        .update(_.revokeCredentials.credentialBatchId := "")

      inside(RevokeCredentialsOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredentials", "credentialBatchId")
          value mustBe ""
      }
    }

    "return error if credential batch id has invalid format" in {
      val cid = "my last credential"
      val invalidOperation = revokeFullBatchOperation
        .update(_.revokeCredentials.credentialBatchId := cid)

      inside(RevokeCredentialsOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredentials", "credentialBatchId")
          value mustBe cid
      }
    }

    "return error if a credential hash to revoke has invalid format" in {
      val invalidSeq = Seq(ByteString.copyFrom("my last credential".getBytes()))
      val invalidOperation = revokeFullBatchOperation
        .update(_.revokeCredentials.credentialsToRevoke := invalidSeq)

      inside(RevokeCredentialsOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredentials", "credentialsToRevoke")
          value mustBe invalidSeq.toString
      }
    }
  }

  "RevokeCredentialsOperation.getCorrectnessData" should {
    "provide the data required for correctness verification" in {
      issuerCreateDIDOperation.applyState().transact(database).value.unsafeRunSync()
      credentialIssueBatchOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = RevokeCredentialsOperation.parse(revokeFullBatchOperation, dummyTimestamp).right.value

      val CorrectnessData(key, previousOperation) = parsedOperation
        .getCorrectnessData("issuing")
        .transact(database)
        .value
        .unsafeRunSync()
        .right
        .value

      key mustBe issuingKeys.publicKey
      previousOperation mustBe Some(credentialIssueBatchOperation.digest)
    }
  }

  "RevokeCredentialsOperation.applyState" should {
    "mark credential batch as revoked in the database" in {
      issuerCreateDIDOperation.applyState().transact(database).value.unsafeRunSync()
      credentialIssueBatchOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = RevokeCredentialsOperation.parse(revokeFullBatchOperation, revocationDate).right.value

      parsedOperation.applyState().transact(database).value.unsafeRunSync().right.value

      val credentialBatch =
        CredentialBatchesDAO
          .findBatch(parsedOperation.credentialBatchId)
          .transact(database)
          .unsafeRunSync()
          .value

      credentialBatch.revokedOn mustBe Some(revocationDate)
    }

    "fail when attempting to revoke an already revoked credential batch" in {
      issuerCreateDIDOperation.applyState().transact(database).value.unsafeRunSync()
      credentialIssueBatchOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = RevokeCredentialsOperation.parse(revokeFullBatchOperation, revocationDate).right.value

      parsedOperation.applyState().transact(database).value.unsafeRunSync().right.value

      val credentialBatch =
        CredentialBatchesDAO
          .findBatch(parsedOperation.credentialBatchId)
          .transact(database)
          .unsafeRunSync()
          .value

      credentialBatch.revokedOn mustBe Some(revocationDate)

      val error = parsedOperation.applyState().transact(database).value.unsafeRunSync()

      error.left.value mustBe a[StateError.BatchAlreadyRevoked]
    }

    "mark specific credentials as revoked in the database" in {
      issuerCreateDIDOperation.applyState().transact(database).value.unsafeRunSync()
      credentialIssueBatchOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation =
        RevokeCredentialsOperation.parse(revokeSpecificCredentialsOperation, revocationDate).right.value

      parsedOperation.applyState().transact(database).value.unsafeRunSync().right.value

      val credentialsRevoked =
        CredentialBatchesDAO
          .findRevokedCredentials(parsedOperation.credentialBatchId)
          .transact(database)
          .unsafeRunSync()

      credentialsRevoked.size mustBe 1
      val (revokedCredHash, revokedAt) = credentialsRevoked.headOption.value
      revokedCredHash mustBe credentialHashToRevoke
      revokedAt mustBe revocationDate

      // the batch itself should not be revoked
      val credentialBatch =
        CredentialBatchesDAO
          .findBatch(parsedOperation.credentialBatchId)
          .transact(database)
          .unsafeRunSync()
          .value

      credentialBatch.revokedOn mustBe empty
    }

    "fail to revoke specific credentials when the batch was already revoked" in {
      issuerCreateDIDOperation.applyState().transact(database).value.unsafeRunSync()
      credentialIssueBatchOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedRevokeBatchOperation =
        RevokeCredentialsOperation.parse(revokeFullBatchOperation, revocationDate).right.value

      parsedRevokeBatchOperation.applyState().value.transact(database).unsafeRunSync().right.value

      val credentialBatch =
        CredentialBatchesDAO
          .findBatch(parsedRevokeBatchOperation.credentialBatchId)
          .transact(database)
          .unsafeRunSync()
          .value

      credentialBatch.revokedOn mustBe Some(revocationDate)

      val parsedOperation =
        RevokeCredentialsOperation.parse(revokeSpecificCredentialsOperation, dummyTimestamp).right.value

      // sanity check
      parsedOperation.credentialBatchId mustBe parsedRevokeBatchOperation.credentialBatchId

      val error = parsedOperation.applyState().transact(database).value.unsafeRunSync().left.value

      error mustBe a[StateError.BatchAlreadyRevoked]

      val credentialsRevoked =
        CredentialBatchesDAO
          .findRevokedCredentials(parsedOperation.credentialBatchId)
          .transact(database)
          .unsafeRunSync()

      credentialsRevoked mustBe empty

      // the batch itself should remain revoked with the same time
      val credentialBatchAfter =
        CredentialBatchesDAO
          .findBatch(parsedRevokeBatchOperation.credentialBatchId)
          .transact(database)
          .unsafeRunSync()
          .value

      credentialBatchAfter.revokedOn mustBe Some(revocationDate)
    }

    "do not update revocation time for specific credentials that were already revoked" in {
      issuerCreateDIDOperation.applyState().transact(database).value.unsafeRunSync()
      credentialIssueBatchOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedFirstOperation =
        RevokeCredentialsOperation.parse(revokeSpecificCredentialsOperation, revocationDate).right.value

      parsedFirstOperation.applyState().value.transact(database).unsafeRunSync().right.value

      val credentialsRevoked =
        CredentialBatchesDAO
          .findRevokedCredentials(parsedFirstOperation.credentialBatchId)
          .transact(database)
          .unsafeRunSync()

      credentialsRevoked.size mustBe 1
      val (revokedCredHash, revokedAt) = credentialsRevoked.headOption.value
      revokedCredHash mustBe credentialHashToRevoke
      revokedAt mustBe revocationDate

      val parsedOSecondperation =
        RevokeCredentialsOperation.parse(revokeSpecificCredentialsOperation, dummyTimestamp).right.value

      // sanity check
      parsedOSecondperation.credentialBatchId mustBe parsedFirstOperation.credentialBatchId

      parsedOSecondperation.applyState().transact(database).value.unsafeRunSync().right.value

      val credentialsRevokedAfter =
        CredentialBatchesDAO
          .findRevokedCredentials(parsedOSecondperation.credentialBatchId)
          .transact(database)
          .unsafeRunSync()

      credentialsRevokedAfter.size mustBe 1
      val (revokedCredHashAfter, revokedAtAfter) = credentialsRevokedAfter.headOption.value
      revokedCredHashAfter mustBe credentialHashToRevoke
      // the time didn't change
      revokedAtAfter mustBe revocationDate

      // the batch itself should not be revoked
      val credentialBatchAfter =
        CredentialBatchesDAO
          .findBatch(parsedFirstOperation.credentialBatchId)
          .transact(database)
          .unsafeRunSync()
          .value

      credentialBatchAfter.revokedOn mustBe empty
    }
  }
}

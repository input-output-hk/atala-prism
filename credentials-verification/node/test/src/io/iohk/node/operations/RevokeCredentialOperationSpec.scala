package io.iohk.node.operations

import java.time.Instant

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.models.{DIDPublicKey, KeyUsage}
import io.iohk.node.repositories.{CredentialsRepository, DIDDataRepository}
import io.iohk.prism.protos.node_models
import org.scalatest.EitherValues._
import org.scalatest.Inside._

import scala.concurrent.duration._

object RevokeCredentialOperationSpec {
  val masterKeys = CreateDIDOperationSpec.masterKeys
  val issuingKeys = CreateDIDOperationSpec.issuingKeys

  lazy val issuerDidKeys = List(
    DIDPublicKey(issuer, "master", KeyUsage.MasterKey, masterKeys.getPublic),
    DIDPublicKey(issuer, "issuing", KeyUsage.IssuingKey, issuingKeys.getPublic)
  )

  lazy val dummyTimestamp = TimestampInfo.dummyTime
  lazy val issuerOperation = CreateDIDOperation.parse(CreateDIDOperationSpec.exampleOperation, dummyTimestamp).right.value
  lazy val credentialIssueOperation =
    IssueCredentialOperation.parse(IssueCredentialOperationSpec.exampleOperation, dummyTimestamp).right.value

  lazy val issuer = issuerOperation.id
  lazy val credentialId = credentialIssueOperation.credentialId

  val revocationDate = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)

  val exampleOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.RevokeCredential(
      value = node_models.RevokeCredentialOperation(
        previousOperationHash = ByteString.copyFrom(credentialIssueOperation.digest.value),
        credentialId = credentialIssueOperation.digest.hexValue,
        revocationDate = None
      )
    )
  )
}

class RevokeCredentialOperationSpec extends PostgresRepositorySpec {

  import RevokeCredentialOperationSpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  override val tables = List("credentials", "public_keys", "did_data")

  override def beforeEach(): Unit = {}

  "RevokeCredentialOperation.parse" should {
    "parse valid RevokeCredential AtalaOperation" in {
      RevokeCredentialOperation.parse(exampleOperation, dummyTimestamp) mustBe a[Right[_, _]]
    }

    "return error when no previous operation is provided" in {
      val invalidOperation = exampleOperation
        .update(_.revokeCredential.previousOperationHash := ByteString.EMPTY)

      inside(RevokeCredentialOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredential", "previousOperationHash")
          value mustBe "0x0"
      }
    }

    "return error when previous operation hash has invalid length" in {
      val bs = ByteString.copyFromUtf8("abc")
      val invalidOperation = exampleOperation
        .update(_.revokeCredential.previousOperationHash := bs)

      inside(RevokeCredentialOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredential", "previousOperationHash")
          value mustBe "0x616263"
      }
    }

    "return error if no credential id is provided" in {
      val invalidOperation = exampleOperation
        .update(_.revokeCredential.credentialId := "")

      inside(RevokeCredentialOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredential", "credentialId")
          value mustBe ""
      }
    }

    "return error if credential id has invalid format" in {
      val cid = "my last credential"
      val invalidOperation = exampleOperation
        .update(_.revokeCredential.credentialId := cid)

      inside(RevokeCredentialOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredential", "credentialId")
          value mustBe cid
      }
    }
  }

  "RevokeCredentialOperation.getCorrectnessData" should {
    "provide the data required for correctness verification" in {
      issuerOperation.applyState().transact(database).value.unsafeRunSync()
      credentialIssueOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = RevokeCredentialOperation.parse(exampleOperation, dummyTimestamp).right.value

      val CorrectnessData(key, previousOperation) = parsedOperation
        .getCorrectnessData("issuing")
        .transact(database)
        .value
        .unsafeRunSync()
        .right
        .value

      key mustBe issuingKeys.getPublic
      previousOperation mustBe Some(credentialIssueOperation.digest)
    }
  }

  "RevokeCredentialOperation.applyState" should {
    "mark credential as revoked in the database" in {
      issuerOperation.applyState().transact(database).value.unsafeRunSync()
      credentialIssueOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = RevokeCredentialOperation.parse(exampleOperation, revocationDate).right.value

      parsedOperation.applyState().transact(database).value.unsafeRunSync().right.value

      val credential = credentialsRepository
        .find(credentialId)
        .value
        .futureValue
        .right
        .value

      credential.revokedOn mustBe Some(revocationDate)
    }
  }

}

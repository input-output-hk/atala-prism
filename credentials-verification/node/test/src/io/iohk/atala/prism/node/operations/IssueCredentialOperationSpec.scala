package io.iohk.atala.prism.node.operations

import java.security.MessageDigest

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.repositories.{CredentialsRepository, DIDDataRepository}
import io.iohk.atala.prism.protos.node_models
import org.scalatest.EitherValues._
import org.scalatest.Inside._

import scala.concurrent.duration._

object IssueCredentialOperationSpec {
  val masterKeys = CreateDIDOperationSpec.masterKeys
  val issuingKeys = CreateDIDOperationSpec.issuingKeys

  lazy val issuerDidKeys = List(
    DIDPublicKey(issuer, "master", KeyUsage.MasterKey, masterKeys.publicKey),
    DIDPublicKey(issuer, "issuing", KeyUsage.IssuingKey, issuingKeys.publicKey)
  )

  lazy val dummyTimestamp = TimestampInfo.dummyTime
  lazy val issuerOperation =
    CreateDIDOperation.parse(CreateDIDOperationSpec.exampleOperation, dummyTimestamp).right.value
  lazy val issuer = issuerOperation.id
  val content = ""
  val contentHash = SHA256Digest(MessageDigest.getInstance("SHA256").digest(content.getBytes)).value

  val exampleOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.IssueCredential(
      value = node_models.IssueCredentialOperation(
        credentialData = Some(
          node_models.CredentialData(
            issuer = issuer.suffix,
            contentHash = ByteString.copyFrom(contentHash)
          )
        )
      )
    )
  )
}

class IssueCredentialOperationSpec extends PostgresRepositorySpec {

  import IssueCredentialOperationSpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  "IssueCredentialOperation.parse" should {
    "parse valid CreateDid AtalaOperation" in {
      IssueCredentialOperation.parse(exampleOperation, dummyTimestamp) mustBe a[Right[_, _]]
    }

    "return error when id is provided" in {
      val providedCredentialId = Array.fill(32)("00").mkString("")
      val invalidOperation = exampleOperation
        .update(_.issueCredential.credentialData.id := providedCredentialId)

      inside(IssueCredentialOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("issueCredential", "credentialData", "id")
          value mustBe providedCredentialId
      }
    }

    "return error when issuer is not provided / empty" in {
      val invalidOperation = exampleOperation
        .update(_.issueCredential.credentialData.issuer := "")

      inside(IssueCredentialOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("issueCredential", "credentialData", "issuer")
          value mustBe ""
      }
    }

    "return error when issuer doesn't have valid form" in {
      val invalidOperation = exampleOperation
        .update(_.issueCredential.credentialData.issuer := "my best friend")

      inside(IssueCredentialOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("issueCredential", "credentialData", "issuer")
          value mustBe "my best friend"
      }
    }

    "return error when content hash is not provided / empty" in {
      val invalidOperation = exampleOperation
        .update(_.issueCredential.credentialData.contentHash := ByteString.EMPTY)

      inside(IssueCredentialOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("issueCredential", "credentialData", "contentHash")
          value mustBe "0x0"
      }
    }

    "return error when hash has invalid length" in {
      val invalidHash = ByteString.copyFrom("abc", "UTF8")
      val invalidOperation = exampleOperation
        .update(_.issueCredential.credentialData.contentHash := invalidHash)

      inside(IssueCredentialOperation.parse(invalidOperation, dummyTimestamp)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("issueCredential", "credentialData", "contentHash")
          value mustBe "0x616263"
      }
    }
  }

  "IssueCredentialOperation.getCorrectnessData" should {
    "provide the key reference be used for signing" in {
      didDataRepository.create(DIDData(issuer, issuerDidKeys, issuerOperation.digest), dummyTimestamp).value.futureValue
      val parsedOperation = IssueCredentialOperation.parse(exampleOperation, dummyTimestamp).right.value

      val CorrectnessData(key, previousOperation) = parsedOperation
        .getCorrectnessData("issuing")
        .transact(database)
        .value
        .unsafeRunSync()
        .right
        .value

      key mustBe issuingKeys.publicKey
      previousOperation mustBe None
    }
  }

  "IssueCredentialOperation.applyState" should {
    "create the credential information in the database" in {
      didDataRepository.create(DIDData(issuer, issuerDidKeys, issuerOperation.digest), dummyTimestamp).value.futureValue
      val parsedOperation = IssueCredentialOperation.parse(exampleOperation, dummyTimestamp).right.value

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
      result mustBe a[Right[_, _]]

      credentialsRepository.find(parsedOperation.credentialId).value.futureValue mustBe a[Right[_, _]]
    }

    "return error when issuer is missing in the DB" in {
      val parsedOperation = IssueCredentialOperation.parse(exampleOperation, dummyTimestamp).right.value

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

    "return error when the credential already exists in the db" in {
      didDataRepository.create(DIDData(issuer, issuerDidKeys, issuerOperation.digest), dummyTimestamp).value.futureValue
      val parsedOperation = IssueCredentialOperation.parse(exampleOperation, dummyTimestamp).right.value

      // first insertion
      parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

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

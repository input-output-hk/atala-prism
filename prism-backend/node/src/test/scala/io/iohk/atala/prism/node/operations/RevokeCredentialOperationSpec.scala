package io.iohk.atala.prism.node.operations

import java.time.Instant

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.node.models.{DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.repositories.{CredentialsRepository, DIDDataRepository}
import io.iohk.atala.prism.protos.node_models
import org.scalatest.OptionValues._
import org.scalatest.Inside._

import scala.concurrent.duration._

object RevokeCredentialOperationSpec {
  val masterKeys = CreateDIDOperationSpec.masterKeys
  val issuingKeys = CreateDIDOperationSpec.issuingKeys

  lazy val issuerDidKeys = List(
    DIDPublicKey(issuer, "master", KeyUsage.MasterKey, masterKeys.publicKey),
    DIDPublicKey(issuer, "issuing", KeyUsage.IssuingKey, issuingKeys.publicKey)
  )

  lazy val dummyTimestamp = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
  lazy val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    dummyTimestamp
  )

  lazy val issuerOperation =
    CreateDIDOperation.parse(CreateDIDOperationSpec.exampleOperation, dummyLedgerData).toOption.value
  lazy val credentialIssueOperation =
    IssueCredentialOperation.parse(IssueCredentialOperationSpec.exampleOperation, dummyLedgerData).toOption.value

  lazy val issuer = issuerOperation.id
  lazy val credentialId = credentialIssueOperation.credentialId

  val revocationDate = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
  val revocationLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    revocationDate
  )

  val exampleOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.RevokeCredential(
      value = node_models.RevokeCredentialOperation(
        previousOperationHash = ByteString.copyFrom(credentialIssueOperation.digest.value.toArray),
        credentialId = credentialIssueOperation.digest.hexValue
      )
    )
  )
}

class RevokeCredentialOperationSpec extends PostgresRepositorySpec {

  import RevokeCredentialOperationSpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  override def beforeEach(): Unit = {}

  "RevokeCredentialOperation.parse" should {
    "parse valid RevokeCredential AtalaOperation" in {
      RevokeCredentialOperation.parse(exampleOperation, dummyLedgerData) mustBe a[Right[_, _]]
    }

    "return error when no previous operation is provided" in {
      val invalidOperation = exampleOperation
        .update(_.revokeCredential.previousOperationHash := ByteString.EMPTY)

      inside(RevokeCredentialOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredential", "previousOperationHash")
          value mustBe "0x0"
      }
    }

    "return error when previous operation hash has invalid length" in {
      val bs = ByteString.copyFromUtf8("abc")
      val invalidOperation = exampleOperation
        .update(_.revokeCredential.previousOperationHash := bs)

      inside(RevokeCredentialOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredential", "previousOperationHash")
          value mustBe "0x616263"
      }
    }

    "return error if no credential id is provided" in {
      val invalidOperation = exampleOperation
        .update(_.revokeCredential.credentialId := "")

      inside(RevokeCredentialOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("revokeCredential", "credentialId")
          value mustBe ""
      }
    }

    "return error if credential id has invalid format" in {
      val cid = "my last credential"
      val invalidOperation = exampleOperation
        .update(_.revokeCredential.credentialId := cid)

      inside(RevokeCredentialOperation.parse(invalidOperation, dummyLedgerData)) {
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

      val parsedOperation = RevokeCredentialOperation.parse(exampleOperation, dummyLedgerData).toOption.value

      val CorrectnessData(key, previousOperation) = parsedOperation
        .getCorrectnessData("issuing")
        .transact(database)
        .value
        .unsafeRunSync()
        .toOption
        .value

      key mustBe issuingKeys.publicKey
      previousOperation mustBe Some(credentialIssueOperation.digest)
    }
  }

  "RevokeCredentialOperation.applyState" should {
    "mark credential as revoked in the database" in {
      issuerOperation.applyState().transact(database).value.unsafeRunSync()
      credentialIssueOperation.applyState().transact(database).value.unsafeRunSync()

      val parsedOperation = RevokeCredentialOperation.parse(exampleOperation, revocationLedgerData).toOption.value

      parsedOperation.applyState().transact(database).value.unsafeRunSync().toOption.value

      val credential = credentialsRepository
        .find(credentialId)
        .value
        .futureValue
        .toOption
        .value

      credential.revokedOn mustBe Some(revocationLedgerData.timestampInfo)
    }
  }

}

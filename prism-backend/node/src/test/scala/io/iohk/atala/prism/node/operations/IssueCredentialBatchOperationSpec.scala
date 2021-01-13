package io.iohk.atala.prism.node.operations

import java.time.Instant

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO
import io.iohk.atala.prism.node.repositories.{CredentialsRepository, DIDDataRepository}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.scalatest.EitherValues._
import org.scalatest.Inside.inside
import org.scalatest.OptionValues.convertOptionToValuable

import scala.concurrent.duration._

object IssueCredentialBatchOperationSpec {
  val masterKeys = CreateDIDOperationSpec.masterKeys
  val issuingKeys = CreateDIDOperationSpec.issuingKeys

  lazy val issuerDidKeys = List(
    DIDPublicKey(issuerDIDSuffix, "master", KeyUsage.MasterKey, masterKeys.publicKey),
    DIDPublicKey(issuerDIDSuffix, "issuing", KeyUsage.IssuingKey, issuingKeys.publicKey)
  )

  lazy val dummyTimestamp = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
  lazy val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    dummyTimestamp
  )

  lazy val issuerCreateDIDOperation =
    CreateDIDOperation.parse(CreateDIDOperationSpec.exampleOperation, dummyLedgerData).toOption.value
  lazy val issuerDIDSuffix = issuerCreateDIDOperation.id
  val content = ""
  val mockMerkleRoot = MerkleRoot(SHA256Digest.compute(content.getBytes))

  val exampleOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.IssueCredentialBatch(
      value = node_models.IssueCredentialBatchOperation(
        credentialBatchData = Some(
          node_models.CredentialBatchData(
            issuerDID = issuerDIDSuffix.value,
            merkleRoot = ByteString.copyFrom(mockMerkleRoot.hash.value.toArray)
          )
        )
      )
    )
  )
}

class IssueCredentialBatchOperationSpec extends PostgresRepositorySpec {

  import IssueCredentialBatchOperationSpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  "IssueCredentialBatchOperation.parse" should {
    "parse valid IssueCredentialBatchOperation AtalaOperation" in {
      IssueCredentialBatchOperation.parse(exampleOperation, dummyLedgerData) mustBe a[Right[_, _]]
    }

    "return error when issuerDID is not provided / empty" in {
      val invalidOperation = exampleOperation
        .update(_.issueCredentialBatch.credentialBatchData.issuerDID := "")

      inside(IssueCredentialBatchOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("issueCredentialBatch", "credentialBatchData", "issuerDID")
          value mustBe ""
      }
    }

    "return error when issuerDID doesn't have valid form" in {
      val invalidOperation = exampleOperation
        .update(_.issueCredentialBatch.credentialBatchData.issuerDID := "my best friend")

      inside(IssueCredentialBatchOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("issueCredentialBatch", "credentialBatchData", "issuerDID")
          value mustBe "my best friend"
      }
    }

    "return error when merkle root is not provided / empty" in {
      val invalidOperation = exampleOperation
        .update(_.issueCredentialBatch.credentialBatchData.merkleRoot := ByteString.EMPTY)

      inside(IssueCredentialBatchOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("issueCredentialBatch", "credentialBatchData", "merkleRoot")
          value mustBe "0x0"
      }
    }

    "return error when hash has invalid length" in {
      val invalidHash = ByteString.copyFrom("abc", "UTF8")
      val invalidOperation = exampleOperation
        .update(_.issueCredentialBatch.credentialBatchData.merkleRoot := invalidHash)

      inside(IssueCredentialBatchOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("issueCredentialBatch", "credentialBatchData", "merkleRoot")
          value mustBe "0x616263"
      }
    }
  }

  "IssueCredentialBatchOperation.getCorrectnessData" should {
    "provide the key reference be used for signing" in {
      didDataRepository
        .create(DIDData(issuerDIDSuffix, issuerDidKeys, issuerCreateDIDOperation.digest), dummyLedgerData)
        .value
        .futureValue
      val parsedOperation = IssueCredentialBatchOperation.parse(exampleOperation, dummyLedgerData).toOption.value

      val CorrectnessData(key, previousOperation) = parsedOperation
        .getCorrectnessData("issuing")
        .transact(database)
        .value
        .unsafeRunSync()
        .toOption
        .value

      key mustBe issuingKeys.publicKey
      previousOperation mustBe None
    }
  }

  "IssueCredentialBatchOperation.applyState" should {
    "create the credential batch information in the database" in {
      didDataRepository
        .create(DIDData(issuerDIDSuffix, issuerDidKeys, issuerCreateDIDOperation.digest), dummyLedgerData)
        .value
        .futureValue
      val parsedOperation = IssueCredentialBatchOperation.parse(exampleOperation, dummyLedgerData).toOption.value

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
      result mustBe a[Right[_, _]]

      val insertedBatch =
        CredentialBatchesDAO
          .findBatch(parsedOperation.credentialBatchId)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .value

      insertedBatch.batchId mustBe parsedOperation.credentialBatchId
      insertedBatch.issuerDIDSuffix mustBe parsedOperation.issuerDIDSuffix
      insertedBatch.merkleRoot mustBe parsedOperation.merkleRoot
      insertedBatch.issuedOn mustBe dummyLedgerData
      insertedBatch.lastOperation mustBe parsedOperation.digest
      insertedBatch.revokedOn mustBe empty
    }

    "return error when issuer is missing in the DB" in {
      val parsedOperation = IssueCredentialBatchOperation.parse(exampleOperation, dummyLedgerData).toOption.value

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
      didDataRepository
        .create(DIDData(issuerDIDSuffix, issuerDidKeys, issuerCreateDIDOperation.digest), dummyLedgerData)
        .value
        .futureValue
      val parsedOperation = IssueCredentialBatchOperation.parse(exampleOperation, dummyLedgerData).toOption.value

      // first insertion
      val resultAttempt1 = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

      resultAttempt1 mustBe a[Right[_, _]]

      val resultAttempt2 = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value

      resultAttempt2 mustBe a[StateError.EntityExists]
    }
  }
}

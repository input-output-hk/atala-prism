package io.iohk.atala.prism.node.operations

import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.{MerkleRoot, Sha256}
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.DataPreparation.{dummyApplyOperationConfig, dummyLedgerData}
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO
import io.iohk.atala.prism.protos.node_models
import org.scalatest.EitherValues._
import org.scalatest.Inside.inside
import org.scalatest.OptionValues.convertOptionToValuable

object IssueCredentialBatchOperationSpec {
  val masterKeys = CreateDIDOperationSpec.masterKeys
  val issuingKeys = CreateDIDOperationSpec.issuingKeys

  lazy val issuerDidKeys = List(
    DIDPublicKey(
      issuerDIDSuffix,
      "master",
      KeyUsage.MasterKey,
      masterKeys.publicKey
    ),
    DIDPublicKey(
      issuerDIDSuffix,
      "issuing",
      KeyUsage.IssuingKey,
      issuingKeys.publicKey
    )
  )

  lazy val issuerCreateDIDOperation =
    CreateDIDOperation
      .parse(CreateDIDOperationSpec.exampleOperation, dummyLedgerData)
      .toOption
      .value
  lazy val issuerDIDSuffix = issuerCreateDIDOperation.id
  val content = ""
  val mockMerkleRoot = new MerkleRoot(Sha256.compute(content.getBytes))

  val exampleOperation = node_models.AtalaOperation(
    operation = node_models.AtalaOperation.Operation.IssueCredentialBatch(
      value = node_models.IssueCredentialBatchOperation(
        credentialBatchData = Some(
          node_models.CredentialBatchData(
            issuerDid = issuerDIDSuffix.getValue,
            merkleRoot = ByteString.copyFrom(mockMerkleRoot.getHash.getValue)
          )
        )
      )
    )
  )
}

class IssueCredentialBatchOperationSpec extends AtalaWithPostgresSpec {

  import IssueCredentialBatchOperationSpec._

  "IssueCredentialBatchOperation.parse" should {
    "parse valid IssueCredentialBatchOperation AtalaOperation" in {
      IssueCredentialBatchOperation.parse(
        exampleOperation,
        dummyLedgerData
      ) mustBe a[Right[_, _]]
    }

    "return error when issuerDID is not provided / empty" in {
      val invalidOperation = exampleOperation
        .update(_.issueCredentialBatch.credentialBatchData.issuerDid := "")

      inside(
        IssueCredentialBatchOperation.parse(invalidOperation, dummyLedgerData)
      ) { case Left(ValidationError.InvalidValue(path, value, _)) =>
        path.path mustBe Vector(
          "issueCredentialBatch",
          "credentialBatchData",
          "issuerDID"
        )
        value mustBe ""
      }
    }

    "return error when issuerDID doesn't have valid form" in {
      val invalidOperation = exampleOperation
        .update(
          _.issueCredentialBatch.credentialBatchData.issuerDid := "my best friend"
        )

      inside(
        IssueCredentialBatchOperation.parse(invalidOperation, dummyLedgerData)
      ) { case Left(ValidationError.InvalidValue(path, value, _)) =>
        path.path mustBe Vector(
          "issueCredentialBatch",
          "credentialBatchData",
          "issuerDID"
        )
        value mustBe "my best friend"
      }
    }

    "return error when merkle root is not provided / empty" in {
      val invalidOperation = exampleOperation
        .update(
          _.issueCredentialBatch.credentialBatchData.merkleRoot := ByteString.EMPTY
        )

      inside(
        IssueCredentialBatchOperation.parse(invalidOperation, dummyLedgerData)
      ) { case Left(ValidationError.InvalidValue(path, value, _)) =>
        path.path mustBe Vector(
          "issueCredentialBatch",
          "credentialBatchData",
          "merkleRoot"
        )
        value mustBe "0x0"
      }
    }

    "return error when hash has invalid length" in {
      val invalidHash = ByteString.copyFrom("abc", "UTF8")
      val invalidOperation = exampleOperation
        .update(
          _.issueCredentialBatch.credentialBatchData.merkleRoot := invalidHash
        )

      inside(
        IssueCredentialBatchOperation.parse(invalidOperation, dummyLedgerData)
      ) { case Left(ValidationError.InvalidValue(path, value, _)) =>
        path.path mustBe Vector(
          "issueCredentialBatch",
          "credentialBatchData",
          "merkleRoot"
        )
        value mustBe "0x616263"
      }
    }
  }

  "IssueCredentialBatchOperation.getCorrectnessData" should {
    "provide the key reference be used for signing" in {
      DataPreparation
        .createDID(
          DIDData(
            issuerDIDSuffix,
            issuerDidKeys,
            Nil,
            Nil,
            issuerCreateDIDOperation.digest
          ),
          dummyLedgerData
        )
      val parsedOperation = IssueCredentialBatchOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

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
    "return state error when there are used different key than issuing key" in {
      DataPreparation
        .createDID(
          DIDData(
            issuerDIDSuffix,
            issuerDidKeys,
            Nil,
            Nil,
            issuerCreateDIDOperation.digest
          ),
          dummyLedgerData
        )
      val parsedOperation = IssueCredentialBatchOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .getCorrectnessData("master")
        .transact(database)
        .value
        .unsafeRunSync()

      result mustBe Left(
        StateError.InvalidKeyUsed(
          "The key type expected is Issuing key. Type used: MasterKey"
        )
      )
    }
    "return state error when unknown keyId is used" in {
      DataPreparation
        .createDID(
          DIDData(
            issuerDIDSuffix,
            issuerDidKeys,
            Nil,
            Nil,
            issuerCreateDIDOperation.digest
          ),
          dummyLedgerData
        )
      val parsedOperation = IssueCredentialBatchOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .getCorrectnessData("issuing3")
        .transact(database)
        .value
        .unsafeRunSync()

      result mustBe Left(StateError.UnknownKey(issuerDIDSuffix, "issuing3"))
    }
  }

  "IssueCredentialBatchOperation.applyState" should {
    "create the credential batch information in the database" in {
      DataPreparation
        .createDID(
          DIDData(
            issuerDIDSuffix,
            issuerDidKeys,
            Nil,
            Nil,
            issuerCreateDIDOperation.digest
          ),
          dummyLedgerData
        )
      val parsedOperation = IssueCredentialBatchOperation
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
      val parsedOperation = IssueCredentialBatchOperation
        .parse(exampleOperation, dummyLedgerData)
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

    "return error when the credential already exists in the db" in {
      DataPreparation
        .createDID(
          DIDData(
            issuerDIDSuffix,
            issuerDidKeys,
            Nil,
            Nil,
            issuerCreateDIDOperation.digest
          ),
          dummyLedgerData
        )

      val parsedOperation = IssueCredentialBatchOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

      // first insertion
      val resultAttempt1 = parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

      resultAttempt1 mustBe a[Right[_, _]]

      val resultAttempt2 = parsedOperation
        .applyState(dummyApplyOperationConfig)
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

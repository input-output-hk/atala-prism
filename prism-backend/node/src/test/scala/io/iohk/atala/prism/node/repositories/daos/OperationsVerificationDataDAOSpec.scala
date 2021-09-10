package io.iohk.atala.prism.node.repositories.daos

import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.kotlin.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.DataPreparation

class OperationsVerificationDataDAOSpec extends AtalaWithPostgresSpec {
  private val previousOperationHashDummy: Sha256Digest = Sha256.compute(DataPreparation.exampleOperation.toByteArray)
  private val signedWithDidIdDummy: DidSuffix = DidSuffix.fromString(":did-suffix-example").get
  private val signedWithKeyIdDummy: String = "master0"

  private val operationHashDummy2: Sha256Digest = Sha256.compute("dummy".getBytes())
  private val didIdDummy2: DidSuffix = DidSuffix.fromDigest(operationHashDummy2)
  private val keyIdDummy2: String = "random-key"

  "OperationsVerificationDataDAO" should {
    "successfully insert a new operation" in {
      OperationsVerificationDataDAO
        .insert(Some(previousOperationHashDummy), signedWithDidIdDummy, signedWithKeyIdDummy)
        .transact(database)
        .unsafeRunSync()

      val countSignedWithKeyId = OperationsVerificationDataDAO
        .countSignedWithKeys(signedWithDidIdDummy, signedWithKeyIdDummy)
        .transact(database)
        .unsafeRunSync()
      countSignedWithKeyId must be(1)

      val countPreviousOperation = OperationsVerificationDataDAO
        .countPreviousOperation(previousOperationHashDummy)
        .transact(database)
        .unsafeRunSync()
      countPreviousOperation must be(1)

      val countDidIds = OperationsVerificationDataDAO
        .countDidIds(signedWithDidIdDummy)
        .transact(database)
        .unsafeRunSync()
      countDidIds must be(1)

      val countNonExistIds = OperationsVerificationDataDAO
        .countSignedWithKeys(didIdDummy2, keyIdDummy2)
        .transact(database)
        .unsafeRunSync()

      countNonExistIds must be(0)
    }
  }
}

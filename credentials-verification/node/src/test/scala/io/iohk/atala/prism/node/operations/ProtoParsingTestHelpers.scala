package io.iohk.atala.prism.node.operations

import java.time.Instant

import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.protos.node_models
import org.scalatest.Assertion
import org.scalatest.Inside._
import org.scalatest.matchers.must.Matchers._
import scalapb.lenses.{Lens, Mutation}

trait ProtoParsingTestHelpers {

  type Repr <: Operation
  protected def exampleOperation: node_models.AtalaOperation
  protected def operationCompanion: OperationCompanion[Repr]

  private val dummyTimestampInfo2 = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)

  protected def signingKeyId = "master"
  protected def signingKey = CreateDIDOperationSpec.masterKeys.privateKey

  protected def missingValueTest[U](
      mutation: Lens[node_models.AtalaOperation, node_models.AtalaOperation] => Mutation[node_models.AtalaOperation],
      expectedPath: Vector[String]
  ): Assertion = {
    val invalidOperation = exampleOperation.update(mutation)
    val signedOperation = BlockProcessingServiceSpec.signOperation(invalidOperation, signingKeyId, signingKey)

    inside(operationCompanion.parse(signedOperation, dummyTimestampInfo2)) {
      case Left(ValidationError.MissingValue(path)) =>
        path.path mustBe expectedPath
    }
  }

  protected def invalidValueTest[U](
      mutation: Lens[node_models.AtalaOperation, node_models.AtalaOperation] => Mutation[node_models.AtalaOperation],
      expectedPath: Vector[String],
      expectedValue: U
  ): Assertion = {
    val invalidOperation = exampleOperation.update(mutation)
    val signedOperation = BlockProcessingServiceSpec.signOperation(invalidOperation, signingKeyId, signingKey)

    inside(operationCompanion.parse(signedOperation, dummyTimestampInfo2)) {
      case Left(ValidationError.InvalidValue(path, value, _)) =>
        path.path mustBe expectedPath
        value mustBe expectedValue
    }
  }

}

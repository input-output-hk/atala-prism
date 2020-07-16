package io.iohk.node.operations

import io.iohk.node.services.BlockProcessingServiceSpec
import io.iohk.prism.protos.node_models
import org.scalatest.Assertion
import org.scalatest.Inside._
import org.scalatest.MustMatchers._
import scalapb.lenses.{Lens, Mutation}

trait ProtoParsingTestHelpers {

  type Repr <: Operation
  protected def exampleOperation: node_models.AtalaOperation
  protected def operationCompanion: OperationCompanion[Repr]

  protected def signingKeyId = "master"
  protected def signingKey = CreateDIDOperationSpec.masterKeys.getPrivate

  protected def missingValueTest[U](
      mutation: Lens[node_models.AtalaOperation, node_models.AtalaOperation] => Mutation[node_models.AtalaOperation],
      expectedPath: Vector[String]
  ): Assertion = {
    val invalidOperation = exampleOperation.update(mutation)
    val signedOperation = BlockProcessingServiceSpec.signOperation(invalidOperation, signingKeyId, signingKey)

    inside(operationCompanion.parse(signedOperation, TimestampInfo.dummyTime)) {
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

    inside(operationCompanion.parse(signedOperation, TimestampInfo.dummyTime)) {
      case Left(ValidationError.InvalidValue(path, value, _)) =>
        path.path mustBe expectedPath
        value mustBe expectedValue
    }
  }

}

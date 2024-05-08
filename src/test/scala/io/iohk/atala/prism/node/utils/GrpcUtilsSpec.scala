package io.iohk.atala.prism.node.utils

import com.google.protobuf.ByteString
import io.iohk.atala.prism.protos.node_api.{OperationOutput, DeactivateDIDOutput, ScheduleOperationsResponse}
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class GrpcUtilsSpec extends AnyWordSpec {
  private val deactivateDidsOperationOutput: OperationOutput = OperationOutput(
    OperationOutput.Result.DeactivateDidOutput(DeactivateDIDOutput()),
    OperationOutput.OperationMaybe.OperationId(ByteString.copyFrom("aba".getBytes))
  )

  "extractSingleOperationOutput" should {
    "correctly extract single output from Vector" in {
      val operationOutput = GrpcUtils.extractSingleOperationOutput(
        ScheduleOperationsResponse(Vector(deactivateDidsOperationOutput))
      )
      operationOutput must be(deactivateDidsOperationOutput)
    }

    "throw error on empty list" in {
      val error = intercept[RuntimeException] {
        GrpcUtils.extractSingleOperationOutput(
          ScheduleOperationsResponse(Seq())
        )
      }
      error.getMessage mustEqual "1 operation output expected but got 0"
    }

    "throw error when more than one output returned" in {
      val error = intercept[RuntimeException] {
        GrpcUtils.extractSingleOperationOutput(
          ScheduleOperationsResponse(Seq(deactivateDidsOperationOutput, deactivateDidsOperationOutput))
        )
      }
      error.getMessage mustEqual "1 operation output expected but got 2"
    }
  }
}

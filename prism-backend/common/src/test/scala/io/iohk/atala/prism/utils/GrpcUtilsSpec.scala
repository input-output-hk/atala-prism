package io.iohk.atala.prism.utils

import com.google.protobuf.ByteString
import io.iohk.atala.prism.protos.node_api.ScheduleOperationsResponse
import io.iohk.atala.prism.protos.node_models.{OperationOutput, RevokeCredentialsOutput}
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class GrpcUtilsSpec extends AnyWordSpec {
  private val revokeCredentialsOperationOutput: OperationOutput = OperationOutput(
    OperationOutput.Result.RevokeCredentialsOutput(RevokeCredentialsOutput()),
    OperationOutput.OperationMaybe.OperationId(ByteString.copyFrom("aba".getBytes))
  )

  "extractSingleOperationOutput" should {
    "correctly extract single output from Vector" in {
      val operationOutput = GrpcUtils.extractSingleOperationOutput(
        ScheduleOperationsResponse(Vector(revokeCredentialsOperationOutput))
      )
      operationOutput must be(revokeCredentialsOperationOutput)
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
          ScheduleOperationsResponse(Seq(revokeCredentialsOperationOutput, revokeCredentialsOperationOutput))
        )
      }
      error.getMessage mustEqual "1 operation output expected but got 2"
    }
  }
}

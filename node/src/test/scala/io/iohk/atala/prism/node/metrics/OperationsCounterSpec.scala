package io.iohk.atala.prism.node.metrics

import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.protos.node_models.{AtalaOperation, SignedAtalaOperation}
import org.scalatest.EitherValues._
import org.scalatest.matchers.must.Matchers

import io.iohk.atala.prism.node.operations.{
  CreateDIDOperationSpec,
  UpdateDIDOperationSpec,
  IssueCredentialBatchOperationSpec,
  RevokeCredentialsOperationSpec,
  ProtocolVersionUpdateOperationSpec,
  DeactivateDIDOperationSpec
}

class OperationsCounterSpec extends AnyWordSpec with Matchers {
  "countReceivedAtalaOperations" should {
    "count all types of operations" in {
      val signingKeyId = "master"
      val signingKey = EC.generateKeyPair().getPrivateKey
      def sign(op: AtalaOperation): SignedAtalaOperation = BlockProcessingServiceSpec.signOperation(
        op,
        signingKeyId,
        signingKey
      )

      val createDidOperation = sign(CreateDIDOperationSpec.exampleOperation)

      // Includes all type of update actions
      val updateDidOperation = sign(UpdateDIDOperationSpec.exampleAllActionsOperation)
      val issueCredentialBatchOperation = sign(IssueCredentialBatchOperationSpec.exampleOperation)
      val revokeCredentialsOperation = sign(RevokeCredentialsOperationSpec.revokeFullBatchOperation)
      val protocolVersionUpdateOperation = sign(
        ProtocolVersionUpdateOperationSpec.protocolUpdateOperation(
          ProtocolVersionUpdateOperationSpec.protocolVersionInfo1
        )
      )
      val deactivateDIDOperation = sign(DeactivateDIDOperationSpec.exampleOperation)

      val operations = List(
        createDidOperation,
        updateDidOperation,
        issueCredentialBatchOperation,
        revokeCredentialsOperation,
        protocolVersionUpdateOperation,
        deactivateDIDOperation
      )

      OperationsCounters.countReceivedAtalaOperations(operations).value mustBe ()

    }
  }
}

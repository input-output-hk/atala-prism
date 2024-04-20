package io.iohk.atala.prism.node.metrics

import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.protos.node_models.{AtalaOperation, SignedAtalaOperation}
import org.scalatest.EitherValues._
import org.scalatest.matchers.must.Matchers
import io.iohk.atala.prism.node.operations.{CreateDIDOperationSpec, DeactivateDIDOperationSpec, ProtocolVersionUpdateOperationSpec, UpdateDIDOperationSpec}

class OperationsCounterSpec extends AnyWordSpec with Matchers {
  "countReceivedAtalaOperations" should {
    "count all types of operations" in {
      val signingKeyId = "master"
      val signingKey = CryptoTestUtils.generateKeyPair().privateKey
      def sign(op: AtalaOperation): SignedAtalaOperation = BlockProcessingServiceSpec.signOperation(
        op,
        signingKeyId,
        signingKey
      )

      val createDidOperation = sign(CreateDIDOperationSpec.exampleOperation)

      // Includes all type of update actions
      val updateDidOperation = sign(UpdateDIDOperationSpec.exampleAllActionsOperation)
      val protocolVersionUpdateOperation = sign(
        ProtocolVersionUpdateOperationSpec.protocolUpdateOperation(
          ProtocolVersionUpdateOperationSpec.protocolVersionInfo1
        )
      )
      val deactivateDIDOperation = sign(DeactivateDIDOperationSpec.exampleOperation)

      val operations = List(
        createDidOperation,
        updateDidOperation,
        protocolVersionUpdateOperation,
        deactivateDIDOperation
      )

      OperationsCounters.countReceivedAtalaOperations(operations).value mustBe ()

    }
  }
}

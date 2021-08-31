package io.iohk.atala.prism.node.models

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.operations.{CreateDIDOperationSpec, IssueCredentialBatchOperationSpec}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.{issuingKeys, masterKeys}
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec.signOperation

class AtalaObjectsDependenciesAnalyzerSpec extends AtalaWithPostgresSpec {

  "AtalaObjectsDependenciesAnalyzer.dependencyInverses" should {
    "find key addition after usage inverse" in {
      val signedCreateDidOperation = signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        masterKeys.getPrivateKey
      )
      val signedIssueCredentialsBatchOperation = signOperation(
        IssueCredentialBatchOperationSpec.exampleOperation,
        "issuing",
        issuingKeys.getPrivateKey
      )
      val atalaObjectInfos = DataPreparation.createAtalaObjectInfos(
        List(
          List(signedIssueCredentialsBatchOperation),
          List(signedCreateDidOperation)
        )
      )

      val analyzer = new AtalaObjectsDependenciesAnalyzer(atalaObjectInfos)
      val inverses = List(
        SHA256Digest.compute(IssueCredentialBatchOperationSpec.exampleOperation.toByteArray) ->
          SHA256Digest.compute(CreateDIDOperationSpec.exampleOperation.toByteArray)
      )

      analyzer.dependencyInverses must be(inverses)
    }
  }

}

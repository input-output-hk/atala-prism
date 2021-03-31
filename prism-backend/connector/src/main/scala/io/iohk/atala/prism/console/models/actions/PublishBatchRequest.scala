package io.iohk.atala.prism.console.models.actions

import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

case class PublishBatchRequest(signedIssueCredentialBatchOp: SignedAtalaOperation)

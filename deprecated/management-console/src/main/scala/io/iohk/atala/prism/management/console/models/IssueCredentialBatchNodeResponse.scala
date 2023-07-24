package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.models.AtalaOperationId

final case class IssueCredentialBatchNodeResponse(
    batchId: CredentialBatchId,
    operationId: AtalaOperationId
)

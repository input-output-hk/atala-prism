package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId

final case class IssueCredentialBatchNodeResponse(
    batchId: CredentialBatchId,
    operationId: AtalaOperationId
)

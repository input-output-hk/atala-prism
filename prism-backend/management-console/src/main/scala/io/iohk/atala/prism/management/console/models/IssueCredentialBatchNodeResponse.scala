package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.protos.common_models.TransactionInfo

final case class IssueCredentialBatchNodeResponse(
    batchId: CredentialBatchId,
    transactionInfo: TransactionInfo
)

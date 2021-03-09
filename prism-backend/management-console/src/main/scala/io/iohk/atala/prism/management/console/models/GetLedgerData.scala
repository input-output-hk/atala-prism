package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.SHA256Digest

final case class GetLedgerData(
    batchId: CredentialBatchId,
    credentialHash: SHA256Digest
)

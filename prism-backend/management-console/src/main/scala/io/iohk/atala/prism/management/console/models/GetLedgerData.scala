package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.Sha256Digest

final case class GetLedgerData(
    batchId: CredentialBatchId,
    credentialHash: Sha256Digest
)

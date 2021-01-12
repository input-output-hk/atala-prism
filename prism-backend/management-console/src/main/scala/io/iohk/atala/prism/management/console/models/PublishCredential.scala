package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.TransactionInfo

case class PublishCredential(
    credentialId: GenericCredential.Id,
    issuanceOperationHash: SHA256Digest,
    nodeCredentialId: String, // TODO: Move node CredentialId class to common
    encodedSignedCredential: String,
    transactionInfo: TransactionInfo
)

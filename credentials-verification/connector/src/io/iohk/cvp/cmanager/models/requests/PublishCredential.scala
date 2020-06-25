package io.iohk.cvp.cmanager.models.requests

import io.iohk.cvp.cmanager.models.GenericCredential
import io.iohk.cvp.crypto.SHA256Digest

case class PublishCredential(
    credentialId: GenericCredential.Id,
    issuanceOperationHash: SHA256Digest,
    nodeCredentialId: String, // TODO: Move node CredentialId class to common
    encodedSignedCredential: String
)

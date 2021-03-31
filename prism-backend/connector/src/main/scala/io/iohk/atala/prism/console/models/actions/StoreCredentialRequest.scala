package io.iohk.atala.prism.console.models.actions

import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.console.models.CredentialExternalId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof

case class StoreCredentialRequest(
    externalId: CredentialExternalId,
    connectionId: ConnectionId,
    merkleProof: MerkleInclusionProof
)

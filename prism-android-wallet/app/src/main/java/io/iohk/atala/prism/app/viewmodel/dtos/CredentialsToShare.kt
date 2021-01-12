package io.iohk.atala.prism.app.viewmodel.dtos

import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.protos.ProofRequest

// TODO We need to remove this class and create an appropriate model for "proof request" and store this data locally with Room
class CredentialsToShare(val credentialsToShare: List<Credential>, val connection: Contact, val proofRequest: ProofRequest, val messageId: String) {
    val credentialsIds: List<String> = credentialsToShare.map { it.credentialId }
}
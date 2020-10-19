package io.iohk.atala.prism.app.viewmodel.dtos

import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.protos.ProofRequest

class CredentialsToShare(val credentialsToShare: List<Credential>, val connection: Contact, val proofRequest: ProofRequest, val messageId: String)
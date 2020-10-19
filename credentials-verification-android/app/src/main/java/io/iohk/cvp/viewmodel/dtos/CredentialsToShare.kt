package io.iohk.cvp.viewmodel.dtos

import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.data.local.db.model.Credential
import io.iohk.atala.prism.protos.ProofRequest

class CredentialsToShare(val credentialsToShare: List<Credential>, val connection: Contact, val proofRequest: ProofRequest, val messageId: String)
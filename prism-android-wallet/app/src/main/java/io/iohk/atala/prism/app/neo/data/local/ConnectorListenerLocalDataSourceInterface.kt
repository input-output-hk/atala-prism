package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest

interface ConnectorListenerLocalDataSourceInterface {

    fun allContacts(): LiveData<List<Contact>>

    suspend fun getContactByConnectionId(connectionId: String): Contact?

    suspend fun updateContact(contact: Contact, issuedCredentials: List<CredentialWithEncodedCredential>)

    suspend fun credentialsByTypes(credentialTypes: List<String>): List<Credential>

    suspend fun insertProofRequest(proofRequest: ProofRequest, credentials: List<Credential>): Long

    suspend fun notRepliedPayIdAddressByMessageId(messageId: String): PayIdAddress?

    suspend fun getPayIdByMessageIdAndStatus(messageId: String, status: PayId.Status): PayId?

    suspend fun updatePayId(payId: PayId)

    suspend fun deletePayId(payId: PayId)

    suspend fun updatePayIdAddress(payIdAddress: PayIdAddress)

    suspend fun deletePayIdAddress(payIdAddress: PayIdAddress)
}

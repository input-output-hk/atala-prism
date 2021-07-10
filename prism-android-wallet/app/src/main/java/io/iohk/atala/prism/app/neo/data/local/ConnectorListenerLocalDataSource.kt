package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao
import io.iohk.atala.prism.app.data.local.db.dao.KycRequestDao
import io.iohk.atala.prism.app.data.local.db.dao.PayIdDao
import io.iohk.atala.prism.app.data.local.db.dao.ProofRequestDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.KycRequest
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConnectorListenerLocalDataSource(
    private val proofRequestDao: ProofRequestDao,
    private val contactDao: ContactDao,
    private val credentialDao: CredentialDao,
    private val payIdDao: PayIdDao,
    private val kycRequestDao: KycRequestDao
) : ConnectorListenerLocalDataSourceInterface {

    override fun allContacts(): LiveData<List<Contact>> = contactDao.all()

    override suspend fun getContactByConnectionId(connectionId: String): Contact? = withContext(Dispatchers.IO) {
        contactDao.getContactByConnectionId(connectionId)
    }

    override suspend fun updateContact(contact: Contact, issuedCredentials: List<CredentialWithEncodedCredential>) = withContext(Dispatchers.IO) {
        contactDao.updateContactSync(contact, issuedCredentials)
    }

    override suspend fun credentialsByTypes(credentialTypes: List<String>): List<Credential> = withContext(Dispatchers.IO) {
        credentialDao.getCredentialsByTypes(credentialTypes)
    }

    override suspend fun insertProofRequest(proofRequest: ProofRequest, credentials: List<Credential>): Long = withContext(Dispatchers.IO) {
        proofRequestDao.insertSync(proofRequest, credentials)
    }

    override suspend fun notRepliedPayIdAddressByMessageId(messageId: String): PayIdAddress? = withContext(Dispatchers.IO) {
        payIdDao.notRepliedPayIdAddressByMessageId(messageId)
    }

    override suspend fun getPayIdByMessageIdAndStatus(
        messageId: String,
        status: PayId.Status
    ): PayId? = withContext(Dispatchers.IO) {
        payIdDao.getPayIdByMessageIdAndStatus(messageId, PayId.Status.WaitingForResponse.value)
    }

    override suspend fun updatePayId(payId: PayId) = withContext(Dispatchers.IO) {
        payIdDao.updatePayId(payId)
    }

    override suspend fun deletePayId(payId: PayId) = withContext(Dispatchers.IO) {
        payIdDao.deletePayId(payId)
    }

    override suspend fun updatePayIdAddress(payIdAddress: PayIdAddress) = withContext(Dispatchers.IO) {
        payIdDao.updatePayIdAddress(payIdAddress)
    }

    override suspend fun deletePayIdAddress(payIdAddress: PayIdAddress) = withContext(Dispatchers.IO) {
        payIdDao.deletePayIdAddress(payIdAddress)
    }

    override suspend fun storeKycRequest(kycRequest: KycRequest) {
        return withContext(Dispatchers.IO) {
            kycRequestDao.insertSync(kycRequest)
        }
    }
}

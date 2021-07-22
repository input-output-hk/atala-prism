package io.iohk.atala.prism.app.neo.data.local

import android.content.Context
import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.core.enums.CredentialType
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao
import io.iohk.atala.prism.app.data.local.db.dao.PayIdDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress
import io.iohk.atala.prism.app.data.local.db.model.PayIdPublicKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PayIdLocalDataSource(
    private val credentialDao: CredentialDao,
    private val contactDao: ContactDao,
    private val payIdDao: PayIdDao,
    context: Context
) : BaseLocalDataSource(context), PayIdLocalDataSourceInterface {

    companion object {
        private const val PAY_ID_CONNECTION_ID = "PAY_ID_CONNECTION_ID"
    }

    override suspend fun storePayIdContact(contact: Contact) = withContext(Dispatchers.IO) {
        contactDao.insert(contact)
        setContactAsAPayIdContact(contact.connectionId)
    }

    override suspend fun setContactAsAPayIdContact(connectionId: String) = withContext(Dispatchers.IO) {
        val editor = preferences.edit()
        editor.putString(PAY_ID_CONNECTION_ID, connectionId)
        editor.apply()
    }

    override suspend fun getCurrentPayIdContact(): Contact? {
        return withContext(Dispatchers.IO) {
            preferences.getString(PAY_ID_CONNECTION_ID, null)?.let {
                contactDao.getContactByConnectionId(it)
            }
        }
    }

    override suspend fun getIdentityCredentials(): List<Credential> {
        return withContext(Dispatchers.IO) {
            credentialDao.getCredentialsByTypes(CredentialType.identityCredentialsTypes)
        }
    }

    override suspend fun getNotIdentityCredentials(): List<Credential> {
        return withContext(Dispatchers.IO) {
            credentialDao.getCredentialsByExcludedTypes(CredentialType.identityCredentialsTypes)
        }
    }

    override suspend fun storePayId(payId: PayId): Long = withContext(Dispatchers.IO) {
        payIdDao.storePayId(payId)
    }

    override suspend fun getPayIdByStatus(status: PayId.Status): PayId? = withContext(Dispatchers.IO) {
        payIdDao.getPayIdByStatus(status.value)
    }

    override fun getPayIdByStatusLiveData(status: PayId.Status): LiveData<PayId?> = payIdDao.getPayIdByStatusLiveData(status.value)

    override suspend fun createPayIdAddress(payIdAddress: PayIdAddress): Long = withContext(Dispatchers.IO) {
        payIdDao.createPayIdAddress(payIdAddress)
    }

    override fun totalOfPayIdAddresses(): LiveData<Int> = payIdDao.totalOfPayIdAddresses()

    override fun registeredPayIdAddresses(): LiveData<List<PayIdAddress>> = payIdDao.registeredPayIdAddresses()

    override suspend fun createPayIdPublicKey(payIdPublicKey: PayIdPublicKey): Long = withContext(Dispatchers.IO) {
        payIdDao.createPayIdPublicKey(payIdPublicKey)
    }

    override fun totalOfPayIdPublicKeys(): LiveData<Int> = payIdDao.totalOfPayIdPublicKeys()

    override fun registeredPayIdPublicKeys(): LiveData<List<PayIdPublicKey>> = payIdDao.registeredPayIdPublicKeys()
}

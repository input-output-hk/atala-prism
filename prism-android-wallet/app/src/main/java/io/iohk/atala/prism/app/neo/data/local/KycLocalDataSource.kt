package io.iohk.atala.prism.app.neo.data.local

import android.content.Context
import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.dao.KycRequestDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.KycRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KycLocalDataSource(private val kycRequestDao: KycRequestDao, private val contactDao: ContactDao, context: Context) : KycLocalDataSourceInterface, BaseLocalDataSource(context) {

    companion object {
        private const val KYC_CONNECTION_ID = "kyc_connection_id"
    }

    override suspend fun storeKycRequest(kycRequest: KycRequest) {
        return withContext(Dispatchers.IO) {
            kycRequestDao.insertSync(kycRequest)
        }
    }

    override fun kycRequestAsync(): LiveData<KycRequest?> = kycRequestDao.firstNotSkipped()

    override suspend fun kycRequestSync(): KycRequest? = withContext(Dispatchers.IO) {
        return@withContext kycRequestDao.firstNotSkippedSync()
    }

    override suspend fun kycContact(): Contact? {
        preferences.getString(KYC_CONNECTION_ID, null)?.let {
            return contactDao.getContactByConnectionId(it)
        }
        return null
    }

    override suspend fun storeKycContact(contact: Contact) {
        withContext(Dispatchers.IO) {
            contactDao.insert(contact)
            val editor = preferences.edit()
            editor.putString(KYC_CONNECTION_ID, contact.connectionId)
            editor.apply()
        }
    }
}

package io.iohk.atala.prism.app.neo.data.local

import android.content.Context
import io.iohk.atala.prism.app.core.enums.CredentialType
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PayIdLocalDataSource(
    private val credentialDao: CredentialDao,
    private val contactDao: ContactDao,
    context: Context
) : BaseLocalDataSource(context), PayIdLocalDataSourceInterface {

    companion object {
        private const val PAY_ID_CONNECTION_ID = "PAY_ID_CONNECTION_ID"
    }

    override suspend fun storePayIdContact(contact: Contact) = withContext(Dispatchers.IO) {
        contactDao.insert(contact)
        val editor = preferences.edit()
        editor.putString(PAY_ID_CONNECTION_ID, contact.connectionId)
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
}

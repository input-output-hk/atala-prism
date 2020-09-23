package io.iohk.cvp.neo.data.local

import io.iohk.cvp.data.local.db.dao.CredentialDao
import io.iohk.cvp.data.local.db.model.Credential

class CredentialsLocalDataSource(private val credentialDao: CredentialDao) : CredentialsLocalDataSourceInterface {
    override suspend fun storeCredentials(credentials: List<Credential>) {
        credentialDao.insertAllCredentials(credentials)
    }
}
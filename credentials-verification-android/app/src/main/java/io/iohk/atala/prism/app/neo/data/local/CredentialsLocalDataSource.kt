package io.iohk.atala.prism.app.neo.data.local

import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao
import io.iohk.atala.prism.app.data.local.db.model.Credential

class CredentialsLocalDataSource(private val credentialDao: CredentialDao) : CredentialsLocalDataSourceInterface {
    override suspend fun storeCredentials(credentials: List<Credential>) {
        credentialDao.insertAllCredentials(credentials)
    }
}
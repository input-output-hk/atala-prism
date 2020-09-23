package io.iohk.cvp.neo.data.local

import io.iohk.cvp.data.local.db.model.Credential

interface CredentialsLocalDataSourceInterface {
    suspend fun storeCredentials(credentials: List<Credential>)
}
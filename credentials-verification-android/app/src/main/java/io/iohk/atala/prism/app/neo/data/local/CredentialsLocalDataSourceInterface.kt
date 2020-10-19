package io.iohk.atala.prism.app.neo.data.local

import io.iohk.atala.prism.app.data.local.db.model.Credential

interface CredentialsLocalDataSourceInterface {
    suspend fun storeCredentials(credentials: List<Credential>)
}
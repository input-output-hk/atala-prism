package io.iohk.atala.prism.app.neo.data

import io.iohk.atala.prism.app.core.enums.CredentialType
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.data.local.CredentialsLocalDataSourceInterface

class PayIdRepository(
    private val credentialsLocalDataSourceInterface: CredentialsLocalDataSourceInterface
) {
    suspend fun getIdentityCredentials(): List<Credential> {
        return credentialsLocalDataSourceInterface.credentialsByTypes(CredentialType.identityCredentialsTypes)
    }

    suspend fun getNotIdentityCredentials(): List<Credential> {
        return credentialsLocalDataSourceInterface.credentialsByExcludedTypes(CredentialType.identityCredentialsTypes)
    }
}

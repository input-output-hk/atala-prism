package io.iohk.atala.prism.app.neo.data.local

import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential

interface PayIdLocalDataSourceInterface {
    suspend fun storePayIdContact(contact: Contact)
    suspend fun getCurrentPayIdContact(): Contact?
    suspend fun getIdentityCredentials(): List<Credential>
    suspend fun getNotIdentityCredentials(): List<Credential>
}

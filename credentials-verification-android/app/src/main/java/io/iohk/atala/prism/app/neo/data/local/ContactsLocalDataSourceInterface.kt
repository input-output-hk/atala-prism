package io.iohk.atala.prism.app.neo.data.local

import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential

interface ContactsLocalDataSourceInterface {
    suspend fun storeContactsWithIssuedCredentials(contactsWithIssuedCredentials: Map<Contact, List<Credential>>)
}
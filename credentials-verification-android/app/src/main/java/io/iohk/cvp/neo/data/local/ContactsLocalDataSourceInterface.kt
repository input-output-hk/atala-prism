package io.iohk.cvp.neo.data.local

import io.iohk.cvp.data.local.db.model.Contact

interface ContactsLocalDataSourceInterface {
    suspend fun storeContacts(contact: List<Contact>)
}
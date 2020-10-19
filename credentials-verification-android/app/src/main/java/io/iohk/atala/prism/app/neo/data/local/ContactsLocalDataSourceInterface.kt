package io.iohk.atala.prism.app.neo.data.local

import io.iohk.atala.prism.app.data.local.db.model.Contact

interface ContactsLocalDataSourceInterface {
    suspend fun storeContacts(contact: List<Contact>)
}
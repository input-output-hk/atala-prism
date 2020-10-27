package io.iohk.atala.prism.app.neo.data.local

import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential

class ContactsLocalDataSource(private val contactDao: ContactDao) : ContactsLocalDataSourceInterface {
    override suspend fun storeContactsWithIssuedCredentials(contactsWithIssuedCredentials: Map<Contact, List<Credential>>) {
        contactDao.insertAll(contactsWithIssuedCredentials)
    }
}
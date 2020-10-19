package io.iohk.atala.prism.app.neo.data.local

import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.model.Contact

class ContactsLocalDataSource(private val contactDao: ContactDao) : ContactsLocalDataSourceInterface {
    override suspend fun storeContacts(contacts: List<Contact>) {
        contactDao.insertAll(contacts)
    }
}
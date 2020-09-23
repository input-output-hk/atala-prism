package io.iohk.cvp.neo.data.local

import io.iohk.cvp.data.local.db.dao.ContactDao
import io.iohk.cvp.data.local.db.model.Contact

class ContactsLocalDataSource(private val contactDao: ContactDao) : ContactsLocalDataSourceInterface {
    override suspend fun storeContacts(contacts: List<Contact>) {
        contactDao.insertAll(contacts)
    }
}
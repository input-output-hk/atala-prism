package io.iohk.cvp.viewmodel.dtos

import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.data.local.db.model.Credential

class ContactToDelete(val credentialsToDelete: List<Credential>, val contact: Contact)
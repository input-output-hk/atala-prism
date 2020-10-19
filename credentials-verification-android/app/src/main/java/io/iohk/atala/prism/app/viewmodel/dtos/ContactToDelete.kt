package io.iohk.atala.prism.app.viewmodel.dtos

import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential

class ContactToDelete(val credentialsToDelete: List<Credential>, val contact: Contact)
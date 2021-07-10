package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.KycRequest

interface KycLocalDataSourceInterface {

    suspend fun storeKycRequest(kycRequest: KycRequest)

    fun kycRequestAsync(): LiveData<KycRequest?>

    suspend fun kycRequestSync(): KycRequest?

    suspend fun kycContact(): Contact?

    suspend fun storeKycContact(contact: Contact)
}

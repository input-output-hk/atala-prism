package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress

interface PayIdLocalDataSourceInterface {
    suspend fun storePayIdContact(contact: Contact)
    suspend fun getCurrentPayIdContact(): Contact?
    suspend fun getIdentityCredentials(): List<Credential>
    suspend fun getNotIdentityCredentials(): List<Credential>
    suspend fun storePayId(payId: PayId): Long
    suspend fun getPayIdByStatus(status: PayId.Status): PayId?
    fun getPayIdByStatusLiveData(status: PayId.Status): LiveData<PayId?>
    suspend fun createPayIdAddress(payIdAddress: PayIdAddress)
    fun firstRegisteredPayIdAddress(): LiveData<PayIdAddress?>
    fun registeredPayIdAddresses(): LiveData<List<PayIdAddress>>
}

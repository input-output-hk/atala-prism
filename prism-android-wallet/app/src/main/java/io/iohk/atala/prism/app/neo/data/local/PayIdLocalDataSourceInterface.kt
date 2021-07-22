package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress
import io.iohk.atala.prism.app.data.local.db.model.PayIdPublicKey

interface PayIdLocalDataSourceInterface {
    suspend fun storePayIdContact(contact: Contact)
    suspend fun getCurrentPayIdContact(): Contact?
    suspend fun getIdentityCredentials(): List<Credential>
    suspend fun getNotIdentityCredentials(): List<Credential>
    suspend fun storePayId(payId: PayId): Long
    suspend fun getPayIdByStatus(status: PayId.Status): PayId?
    fun getPayIdByStatusLiveData(status: PayId.Status): LiveData<PayId?>
    fun totalOfPayIdAddresses(): LiveData<Int>
    fun registeredPayIdAddresses(): LiveData<List<PayIdAddress>>
    fun totalOfPayIdPublicKeys(): LiveData<Int>
    fun registeredPayIdPublicKeys(): LiveData<List<PayIdPublicKey>>
}

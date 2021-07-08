package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PayIdLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.model.UserProfile

class PreferencesRepository(
    private val payIdLocalDataSource: PayIdLocalDataSourceInterface,
    private val localDataSource: ContactsLocalDataSourceInterface,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) : BaseRepository(sessionLocalDataSource, preferencesLocalDataSource) {

    val payId: LiveData<PayId?> = payIdLocalDataSource.getPayIdByStatusLiveData(PayId.Status.Registered)

    fun getAvailableCustomDateFormats(): List<CustomDateFormat> = CustomDateFormat.values().toList()

    fun getDefaultDateFormat(): CustomDateFormat {
        return CustomDateFormat.DDMMYYYY
    }

    suspend fun saveCustomDateFormat(dateFormat: CustomDateFormat) {
        preferencesLocalDataSource.storeCustomDateFormat(dateFormat)
    }

    suspend fun resetData() {
        /*
        * when removing the contacts, tables ActivityHistory, Credential and ProofRequest will be remove in cascaded
        * */
        localDataSource.removeAllContacts()
    }

    suspend fun storeUserProfile(userProfile: UserProfile) = preferencesLocalDataSource.storeUserProfile(userProfile)
}

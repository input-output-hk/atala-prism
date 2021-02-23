package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.utils.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionRepository(
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) : BaseRepository(sessionLocalDataSource, preferencesLocalDataSource) {

    private val _sessionDataHasStored = MutableLiveData<Boolean>()

    // Exposed only to be observed
    val sessionDataHasStored: LiveData<Boolean> = _sessionDataHasStored

    suspend fun fetchSession() {
        /*
         * fetching data inside a Coroutine in order to don't interrupt the UI thread
        * */
        withContext(Dispatchers.IO) {
            _sessionDataHasStored.postValue(sessionLocalDataSource.hasData())
        }
    }

    /*
     * Generate a new Mnemonic List
     */
    suspend fun getNewMnemonicList(): List<String> {
        return withContext(Dispatchers.Default) {
            return@withContext CryptoUtils.generateMnemonicList()
        }
    }

    suspend fun storeSession(mnemonicList: List<String>) {
        return withContext(Dispatchers.IO) {
            sessionLocalDataSource.storeSessionData(mnemonicList)
        }
    }
}

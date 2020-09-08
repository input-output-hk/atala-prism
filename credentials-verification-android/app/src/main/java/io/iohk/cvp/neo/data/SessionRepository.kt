package io.iohk.cvp.neo.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.iohk.cvp.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.cvp.utils.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionRepository(private val localDataSource: SessionLocalDataSourceInterface) {

    private val _sessionDataHasStored = MutableLiveData<Boolean>()

    // Exposed only to be observed
    val sessionDataHasStored: LiveData<Boolean> = _sessionDataHasStored

    suspend fun fetchSession() {
        /*
         * fetching data inside a Coroutine in order to don't interrupt the UI thread
        * */
        withContext(Dispatchers.IO) {
            _sessionDataHasStored.postValue(localDataSource.hasData())
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
}
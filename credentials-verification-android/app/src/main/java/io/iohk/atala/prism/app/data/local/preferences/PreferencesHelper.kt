package io.iohk.atala.prism.app.data.local.preferences

import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair

/* This helper is extended by DataManager(alongside DbHelper and ApiHelper) so i can
     put together all my data implementation in the same place. This is called Datamanager pattern.*/
interface PreferencesHelper {
    fun getCurrentIndex(): Int
    fun getMnemonicList(): List<String>
    fun increaseIndex()
    fun saveMnemonics(phrasesList: MutableList<String>)
    fun saveIndex(lastIndex: Int)
    fun getKeyPairFromPath(keyDerivationPath: String): ECKeyPair
    fun saveCustomDateFormat(dateFormat: CustomDateFormat)
    fun getDefaultDateFormat(): CustomDateFormat
    suspend fun getCurrentDateFormat(): CustomDateFormat
    suspend fun getCustomDateFormats(): List<CustomDateFormat>
}

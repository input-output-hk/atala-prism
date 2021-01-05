package io.iohk.atala.prism.app.data.local.preferences

import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferencesHelper @Inject constructor(private val preferences: Preferences) : PreferencesHelper {
    override fun getCurrentIndex(): Int {
        return preferences.currentIndex
    }

    override fun getMnemonicList(): List<String> {
        return preferences.mnemonicList
    }

    override fun increaseIndex() {
        preferences.increaseIndex()
    }

    override fun getKeyPairFromPath(keyDerivationPath: String): ECKeyPair {
        return preferences.getKeyPairFromPath(keyDerivationPath)
    }

    override fun saveCustomDateFormat(dateFormat: CustomDateFormat) {
        preferences.saveCustomDateFormat(dateFormat)
    }

    override fun getDefaultDateFormat(): CustomDateFormat {
        return preferences.getDefaultDateFormat()
    }

    override suspend fun getCurrentDateFormat(): CustomDateFormat {
        return preferences.getCurrentDateFormat()
    }

    override suspend fun getCustomDateFormats(): List<CustomDateFormat> {
        return CustomDateFormat.values().toList()
    }

    override fun saveMnemonics(phrasesList: MutableList<String>) {
        preferences.saveMnemonicList(phrasesList)
    }

    override fun saveIndex(lastIndex: Int) {
        preferences.saveIndex(lastIndex)
    }
}
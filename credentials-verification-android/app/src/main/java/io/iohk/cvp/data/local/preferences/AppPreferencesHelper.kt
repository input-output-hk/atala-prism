package io.iohk.cvp.data.local.preferences

import io.iohk.atala.crypto.japi.ECKeyPair
import io.iohk.cvp.views.Preferences
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
}

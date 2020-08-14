package io.iohk.cvp.data.local.preferences

import io.iohk.atala.crypto.japi.ECKeyPair

/* This helper is extended by DataManager(alongside DbHelper and ApiHelper) so i can
     put together all my data implementation in the same place. This is called Datamanager pattern.*/
interface PreferencesHelper {
    fun getCurrentIndex(): Int
    fun getMnemonicList(): List<String>
    fun increaseIndex()
    fun getKeyPairFromPath(keyDerivationPath: String): ECKeyPair
}

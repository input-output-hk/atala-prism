package io.iohk.cvp.neo.data.local

import android.content.Context
import io.iohk.cvp.utils.CryptoUtils

class SessionLocalDataSource(context: Context) : BaseLocalDataSource(context), SessionLocalDataSourceInterface {

    companion object {
        private const val MNEMONIC_LIST_PREFERENCE = "mnemonic_list"
    }

    override fun hasData(): Boolean {
        return preferences.contains(MNEMONIC_LIST_PREFERENCE)
    }

    override fun storeSessionData(mnemonicList: List<String>) {
        val stringList: String = mnemonicList.joinToString(",")
        val editor = preferences.edit()
        editor.putString(MNEMONIC_LIST_PREFERENCE, stringList)
        editor.apply()
    }
}
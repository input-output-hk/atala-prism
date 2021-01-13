package io.iohk.atala.prism.app.neo.data.local

import android.content.Context

class SessionLocalDataSource(context: Context) : BaseLocalDataSource(context), SessionLocalDataSourceInterface {

    companion object {
        private const val MNEMONIC_LIST_PREFERENCE = "mnemonic_list"
        private const val LAST_SYNCED_INDEX = "current_value_index"
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

    override fun getSessionData(): List<String>? {
        return preferences.getString(MNEMONIC_LIST_PREFERENCE, null)?.let { value ->
            value.split(",".toRegex())
        }
    }

    override fun storeLastSyncedIndex(index: Int) {
        val editor = preferences.edit()
        editor.putInt(LAST_SYNCED_INDEX, index)
        editor.apply()
    }

    override fun getLastSyncedIndex(): Int {
        return preferences.getInt(LAST_SYNCED_INDEX, -1)
    }
}
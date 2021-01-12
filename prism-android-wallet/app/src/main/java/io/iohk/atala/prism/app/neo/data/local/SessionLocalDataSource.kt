package io.iohk.atala.prism.app.neo.data.local

import android.content.Context
import io.iohk.atala.prism.app.neo.model.BackendConfig

class SessionLocalDataSource(context: Context) : BaseLocalDataSource(context), SessionLocalDataSourceInterface {

    companion object {
        private const val MNEMONIC_LIST_PREFERENCE = "mnemonic_list"
        private const val LAST_SYNCED_INDEX = "current_value_index"
        private const val CUSTOM_BACKEND_URL = "backend_ip"
        private const val CUSTOM_BACKEND_PORT = "backend_port"
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

    override fun storeCustomBackendConfig(config: BackendConfig) {
        val editor = preferences.edit()
        editor.putString(CUSTOM_BACKEND_URL, config.url)
        editor.putInt(CUSTOM_BACKEND_PORT, config.port)
        editor.apply()
    }

    override fun getCustomBackendConfig(): BackendConfig? {
        val url = preferences.getString(CUSTOM_BACKEND_URL, null)
        val port = preferences.getInt(CUSTOM_BACKEND_PORT, -1)
        if (port == -1 || url == null) {
            return null
        }
        return BackendConfig(url!!, port)
    }
}
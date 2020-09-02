package io.iohk.cvp.neo.data.local

import android.content.Context

class SessionLocalDataSource(context: Context) : BaseLocalDataSource(context), SessionLocalDataSourceInterface {

    companion object {
        private const val MNEMONIC_LIST_PREFERENCE = "mnemonic_list"
    }

    override fun hasData(): Boolean {
        return preferences.contains(MNEMONIC_LIST_PREFERENCE)
    }
}
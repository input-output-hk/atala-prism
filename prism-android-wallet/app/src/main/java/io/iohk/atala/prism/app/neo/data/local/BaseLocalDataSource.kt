package io.iohk.atala.prism.app.neo.data.local

import android.content.Context
import android.content.SharedPreferences

open class BaseLocalDataSource(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "IOHK.ATALA.CREDENTIAL.VERIFICATION"
    }

    protected val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

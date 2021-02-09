package io.iohk.atala.prism.app.data.local.preferences

import android.content.Context
import io.iohk.atala.prism.app.core.exception.WrongPinLengthException
import org.apache.commons.lang3.StringUtils

/*
* TODO this class has to be deleted and replaced by PreferencesRepository this will be done
*  when finishing refactoring some screens that currently do not have an appropriate viewmodel
* */
class Preferences(private val context: Context) {

    fun saveSecurityPin(pin: SecurityPin) {
        editString(pin.pinString, SECURITY_PIN)
    }

    @get:Throws(WrongPinLengthException::class)
    val securityPin: SecurityPin
        get() = SecurityPin(getString(SECURITY_PIN))
    val isPinConfigured: Boolean
        get() = StringUtils.isNotBlank(getString(SECURITY_PIN))

    fun saveSecurityTouch(enable: Boolean?) {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(SECURITY_TOUCH_ENABLED, enable!!)
        editor.apply()
    }

    val securityTouch: Boolean
        get() {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(SECURITY_TOUCH_ENABLED, false)
        }

    private fun editString(valueToAdd: String, prefKey: String) {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(prefKey, valueToAdd)
        editor.apply()
    }

    fun getString(key: String?): String? {
        val sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences
                .getString(key, "")
    }

    fun saveBackendData(ip: String?, port: String) {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(BACKEND_IP, ip)
        val portValue = if (port == "") 0 else Integer.valueOf(port)
        editor.putInt(BACKEND_PORT, portValue)
        editor.apply()
    }

    fun getInt(key: String?): Int {
        val sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences
                .getInt(key, 0)
    }

    companion object {
        const val BACKEND_IP = "backend_ip"
        const val BACKEND_PORT = "backend_port"
        private const val MY_PREFS_NAME = "IOHK.ATALA.CREDENTIAL.VERIFICATION"
        const val SECURITY_PIN = "security_pin"
        const val SECURITY_TOUCH_ENABLED = "security_touch_enabled"
    }
}
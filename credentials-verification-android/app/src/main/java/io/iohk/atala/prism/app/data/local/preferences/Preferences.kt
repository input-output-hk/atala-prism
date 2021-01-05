package io.iohk.atala.prism.app.data.local.preferences

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import io.iohk.atala.prism.app.core.exception.WrongPinLengthException
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.data.local.preferences.models.customDateFormatFrom
import io.iohk.atala.prism.app.neo.common.extensions.Bitmap
import io.iohk.atala.prism.app.neo.common.extensions.toEncodedBase64String
import io.iohk.atala.prism.app.utils.CryptoUtils.Companion.getKeyPairFromPath
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import java.util.*
import java.util.stream.Collectors


class Preferences(private val context: Context) {
    val isPrivateKeyStored: Boolean
        get() {
            val sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            val pk = Optional.ofNullable(sharedPreferences.getString(MNEMONIC_LIST, null))
            return pk.isPresent
        }

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

    suspend fun saveUserProfile(name: String?, country: String?, email: String?, profileImage: Bitmap?) {
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(USER_PROFILE_NAME, name)
            editor.putString(USER_PROFILE_COUNTRY, country)
            editor.putString(USER_PROFILE_EMAIL, email)
            editor.putString(USER_PROFILE_IMAGE, profileImage?.toEncodedBase64String(Bitmap.CompressFormat.JPEG, 70))
            editor.apply()
        }
    }

    fun getString(key: String?): String? {
        val sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences
                .getString(key, "")
    }

    suspend fun getProfileImage(): Bitmap? {
        getString(USER_PROFILE_IMAGE)?.let {
            return Bitmap(encodedBase64String = it)
        } ?: kotlin.run {
            return null
        }
    }

    fun saveBackendData(ip: String?, port: String) {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(BACKEND_IP, ip)
        val portValue = if (port == "") 0 else Integer.valueOf(port)
        editor.putInt(BACKEND_PORT, portValue)
        editor.apply()
    }

    fun saveInt(key: String?, value: Int) {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getInt(key: String?): Int {
        val sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences
                .getInt(key, 0)
    }

    var isFirstLaunch: Boolean
        get() {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(FIRST_LAUNCH, true)
        }
        set(firstLaunch) {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean(FIRST_LAUNCH, firstLaunch)
            editor.apply()
        }
    val currentIndex: Int
        get() {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(CURRENT_VALUE_INDEX, -1)
        }
    val mnemonicList: List<String>
        get() = getOrderedCollection(MNEMONIC_LIST)

    fun saveMnemonicList(mnemonicList: List<String>) {
        saveOrderedArray(mnemonicList, MNEMONIC_LIST)
    }

    private fun saveOrderedArray(list: List<String>, key: String) {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        val listAsString = list.stream()
                .collect(Collectors.joining(DELIMIT_CHARACTER))
        val editor = prefs.edit()
        editor.putString(key, listAsString)
        editor.apply()
    }

    fun getOrderedCollection(key: String?): List<String> {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        val listAsString = prefs.getString(key, "")
        val myNewList = listAsString!!.split(DELIMIT_CHARACTER.toRegex()).toTypedArray()
        return listOf(*myNewList)
    }

    fun increaseIndex() {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        var currentIndex = prefs.getInt(CURRENT_VALUE_INDEX, -1)
        val editor = prefs.edit()
        editor.putInt(CURRENT_VALUE_INDEX, ++currentIndex)
        editor.apply()
    }

    fun getKeyPairFromPath(keyDerivationPath: String?): ECKeyPair {
        return getKeyPairFromPath(keyDerivationPath!!, mnemonicList)
    }

    fun saveIndex(lastIndex: Int) {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt(CURRENT_VALUE_INDEX, lastIndex)
        editor.apply()
    }

    fun saveCustomDateFormat(dateFormat: CustomDateFormat) {
        saveInt(USER_CUSTOM_DATE_FORMAT, dateFormat.value)
    }

    fun getDefaultDateFormat(): CustomDateFormat {
        return CustomDateFormat.DDMMYYYY
    }

    fun getCurrentDateFormat(): CustomDateFormat {
        val sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        val value = sharedPreferences.getInt(USER_CUSTOM_DATE_FORMAT, -1)
        return customDateFormatFrom(value, getDefaultDateFormat())
    }

    companion object {
        const val USER_PROFILE_NAME = "user_profile_name"
        const val USER_PROFILE_COUNTRY = "user_profile_country"
        const val USER_PROFILE_IMAGE = "user_profile_image"
        const val USER_PROFILE_EMAIL = "user_profile_email"
        const val CONNECTION_TOKEN_TO_ACCEPT = "connection_token_to_accept"
        const val BACKEND_IP = "backend_ip"
        const val BACKEND_PORT = "backend_port"
        private const val MY_PREFS_NAME = "IOHK.ATALA.CREDENTIAL.VERIFICATION"
        const val SECURITY_PIN = "security_pin"
        const val SECURITY_TOUCH_ENABLED = "security_touch_enabled"
        const val USER_CUSTOM_DATE_FORMAT = "user_custom_date_format"

        // This key get used always that app start, this help to detect when app is starting and avoid to call UnlockActivity before the first activity is launched.
        const val FIRST_LAUNCH = "first_launch"
        private const val CURRENT_VALUE_INDEX = "current_value_index"
        private const val MNEMONIC_LIST = "mnemonic_list"
        private const val DELIMIT_CHARACTER = ","
    }
}
package io.iohk.atala.prism.app.neo.data.local

import android.content.Context
import android.graphics.Bitmap
import io.iohk.atala.prism.app.data.local.preferences.SecurityPin
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.data.local.preferences.models.customDateFormatFrom
import io.iohk.atala.prism.app.neo.common.extensions.Bitmap
import io.iohk.atala.prism.app.neo.common.extensions.toEncodedBase64String
import io.iohk.atala.prism.app.neo.model.BackendConfig
import io.iohk.atala.prism.app.neo.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferencesLocalDataSource(context: Context) : BaseLocalDataSource(context), PreferencesLocalDataSourceInterface {

    companion object {
        private const val SECURITY_PIN = "security_pin"
        private const val SECURITY_TOUCH_ENABLED = "security_touch_enabled"
        private const val USER_PROFILE_NAME = "user_profile_name"
        private const val USER_PROFILE_COUNTRY = "user_profile_country"
        private const val USER_PROFILE_IMAGE = "user_profile_image"
        private const val USER_PROFILE_EMAIL = "user_profile_email"
        private const val USER_CUSTOM_DATE_FORMAT = "user_custom_date_format"
        private const val CUSTOM_BACKEND_URL = "backend_ip"
        private const val CUSTOM_BACKEND_PORT = "backend_port"
    }

    override suspend fun storeSecurityPin(securityPin: SecurityPin) {
        return withContext(Dispatchers.IO) {
            val editor = preferences.edit()
            editor.putString(SECURITY_PIN, securityPin.pinString)
            editor.apply()
        }
    }

    override suspend fun getSecurityPin(): SecurityPin? {
        return withContext(Dispatchers.IO) {
            preferences.getString(SECURITY_PIN, null)?.let {
                return@withContext SecurityPin(it)
            }
            return@withContext null
        }
    }

    override suspend fun storeSecurityTouch(enable: Boolean) {
        return withContext(Dispatchers.IO) {
            val editor = preferences.edit()
            editor.putBoolean(SECURITY_TOUCH_ENABLED, enable)
            editor.apply()
        }
    }

    override suspend fun getSecurityTouch(): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext preferences.getBoolean(SECURITY_TOUCH_ENABLED, false)
        }
    }

    override suspend fun storeUserProfile(userProfile: UserProfile) {
        return withContext(Dispatchers.IO) {
            val editor = preferences.edit()
            editor.putString(USER_PROFILE_NAME, userProfile.name)
            editor.putString(USER_PROFILE_COUNTRY, userProfile.country)
            editor.putString(USER_PROFILE_EMAIL, userProfile.email)
            editor.putString(USER_PROFILE_IMAGE, userProfile.profileImage?.toEncodedBase64String(Bitmap.CompressFormat.JPEG, 70))
            editor.apply()
        }
    }

    override suspend fun getUserProfile(): UserProfile {
        return withContext(Dispatchers.IO) {
            return@withContext UserProfile(
                name = preferences.getString(USER_PROFILE_NAME, null),
                country = preferences.getString(USER_PROFILE_COUNTRY, null),
                email = preferences.getString(USER_PROFILE_EMAIL, null),
                profileImage = preferences.getString(USER_PROFILE_IMAGE, null)?.let {
                    return@let Bitmap(encodedBase64String = it)
                }
            )
        }
    }

    override suspend fun storeCustomDateFormat(customDateFormat: CustomDateFormat) {
        return withContext(Dispatchers.IO) {
            val editor = preferences.edit()
            editor.putInt(USER_CUSTOM_DATE_FORMAT, customDateFormat.value)
            editor.apply()
        }
    }

    override suspend fun getCustomDateFormat(): CustomDateFormat {
        return withContext(Dispatchers.IO) {
            return@withContext customDateFormatFrom(preferences.getInt(USER_CUSTOM_DATE_FORMAT, -1), getDefaultDateFormat())
        }
    }

    override fun getDefaultDateFormat(): CustomDateFormat {
        return CustomDateFormat.DDMMYYYY
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

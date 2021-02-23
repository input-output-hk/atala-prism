package io.iohk.atala.prism.app.neo.data.local

import io.iohk.atala.prism.app.data.local.preferences.SecurityPin
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.model.BackendConfig
import io.iohk.atala.prism.app.neo.model.UserProfile

interface PreferencesLocalDataSourceInterface {
    suspend fun storeSecurityPin(securityPin: SecurityPin)
    suspend fun getSecurityPin(): SecurityPin?
    suspend fun storeSecurityTouch(enable: Boolean)
    suspend fun getSecurityTouch(): Boolean
    suspend fun storeUserProfile(userProfile: UserProfile)
    suspend fun getUserProfile(): UserProfile
    suspend fun storeCustomDateFormat(customDateFormat: CustomDateFormat)
    suspend fun getCustomDateFormat(): CustomDateFormat
    fun getDefaultDateFormat(): CustomDateFormat
    fun storeCustomBackendConfig(config: BackendConfig)
    fun getCustomBackendConfig(): BackendConfig?
}

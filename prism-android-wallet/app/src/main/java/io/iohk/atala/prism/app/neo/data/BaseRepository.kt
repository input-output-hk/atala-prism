package io.iohk.atala.prism.app.neo.data

import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
* Here is the data that will be shared between all repositories
* */
open class BaseRepository(
    protected val sessionLocalDataSource: SessionLocalDataSourceInterface,
    protected val preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) {

    suspend fun getCustomDateFormat(): CustomDateFormat {
        return preferencesLocalDataSource.getCustomDateFormat()
    }

    suspend fun isSecurityPinConfigured(): Boolean {
        return preferencesLocalDataSource.getSecurityPin() != null
    }

    suspend fun sessionData(): List<String>? = withContext(Dispatchers.IO) {
        sessionLocalDataSource.getSessionData()
    }
}

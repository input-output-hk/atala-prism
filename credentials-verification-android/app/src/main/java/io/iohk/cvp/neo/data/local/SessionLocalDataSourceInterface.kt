package io.iohk.cvp.neo.data.local

import io.iohk.cvp.neo.model.BackendConfig

interface SessionLocalDataSourceInterface {
    /**
     * Return true when there is session data stored locally
     */
    fun hasData(): Boolean

    fun storeSessionData(mnemonicList: List<String>)

    fun storeLastSyncedIndex(index: Int)

    fun getLastSyncedIndex(): Int

    fun storeCustomBackendConfig(config: BackendConfig)

    fun getCustomBackendConfig(): BackendConfig?
}
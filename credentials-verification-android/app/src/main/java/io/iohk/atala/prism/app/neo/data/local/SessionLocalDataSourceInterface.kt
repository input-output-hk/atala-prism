package io.iohk.atala.prism.app.neo.data.local

import io.iohk.atala.prism.app.neo.model.BackendConfig

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
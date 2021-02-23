package io.iohk.atala.prism.app.neo.data.local

interface SessionLocalDataSourceInterface {
    /**
     * Return true when there is session data stored locally
     */
    fun hasData(): Boolean

    fun storeSessionData(mnemonicList: List<String>)

    fun getSessionData(): List<String>?

    fun storeLastSyncedIndex(index: Int)

    fun getLastSyncedIndex(): Int

    fun increaseSyncedIndex(): Int
}

package io.iohk.cvp.neo.data.local

interface SessionLocalDataSourceInterface {
    /**
     * Return true when there is session data stored locally
     */
    fun hasData(): Boolean

    fun storeSessionData(mnemonicList: List<String>)
}
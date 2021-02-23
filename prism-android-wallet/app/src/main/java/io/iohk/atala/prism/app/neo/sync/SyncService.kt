package io.iohk.atala.prism.app.neo.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SyncService : Service() {

    companion object {
        private var syncAdapter: SyncAdapter? = null
        private val syncAdapterLock = Any()
    }

    override fun onCreate() {
        synchronized(syncAdapterLock) {
            syncAdapter = syncAdapter ?: SyncAdapter(this, true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return syncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }
}

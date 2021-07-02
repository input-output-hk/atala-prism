package io.iohk.atala.prism.app.core

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dagger.android.DaggerService
import io.iohk.atala.prism.app.neo.data.ConnectorListenerRepository
import javax.inject.Inject

/**
 * This is a service that is basically listening and processing all the messages that arrive
 * through the connector using the [ConnectorListenerRepository]
* */
class ConnectorListenerService : DaggerService() {
    @Inject
    lateinit var connectorListenerRepository: ConnectorListenerRepository

    private val binder = Binder()

    private var streamIsStarted = false

    override fun onBind(intent: Intent?): IBinder? {
        if (!streamIsStarted) {
            // Start gRPC data streams
            connectorListenerRepository.startConnectionsStreams()
            streamIsStarted = true
        }
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // Stop gRPC data streams
        connectorListenerRepository.stopConnectionsStreams()
        streamIsStarted = false
        return super.onUnbind(intent)
    }
}

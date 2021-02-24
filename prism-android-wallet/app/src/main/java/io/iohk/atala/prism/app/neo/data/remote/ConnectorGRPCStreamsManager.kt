package io.iohk.atala.prism.app.neo.data.remote

import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.utils.CryptoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConnectorGRPCStreamsManager {

    companion object {
        private const val RECONNECTION_TASK_TIME_MILLIS = 5000L

        // For Singleton instantiation
        @Volatile
        private var instance: ConnectorGRPCStreamsManager? = null

        fun getInstance(api: ConnectorRemoteDataSource, onMessageListener: ConnectorGRPCStream.OnMessageListener): ConnectorGRPCStreamsManager {
            return instance ?: synchronized(this) {
                instance ?: build(api, onMessageListener).also { instance = it }
            }
        }

        private fun build(api: ConnectorRemoteDataSource, onMessageListener: ConnectorGRPCStream.OnMessageListener): ConnectorGRPCStreamsManager {
            val obj = ConnectorGRPCStreamsManager()
            obj.api = api
            obj.onMessageListener = onMessageListener
            return obj
        }
    }

    private lateinit var api: ConnectorRemoteDataSource

    private val streams: MutableMap<String, ConnectorGRPCStream> = mutableMapOf()

    private lateinit var onMessageListener: ConnectorGRPCStream.OnMessageListener

    private var reconnectionScope: CoroutineScope? = null

    /**
     * Updates the map of data streams based on a list of contacts, giving priority to the list of contacts,
     * that is, if there is a stream of data with certain connectionId in the map but there is no contact
     * with that connectionId that data stream should be stopped and removed from the map. And if there is
     * a contact with a certain connectionId but there is no data stream with that connectionId, one must
     * be created.
     *
     * @param contacts [List] of [Contact]
     * @param mnemonicList [List] of [String]  List of words to create the keypair to establish a connection
     */
    fun handleConnections(contacts: List<Contact>, mnemonicList: List<String>) {
        return synchronized(this) {
            stopReconnectionTask()
            // stop removed streams
            streams.forEach { (key, item) ->
                if (contacts.find { it.connectionId == key } == null) {
                    item.stop()
                }
            }
            val newConnectionsMap: MutableMap<String, ConnectorGRPCStream> = mutableMapOf()
            contacts.forEach { contact ->
                if (streams.containsKey(contact.connectionId)) {
                    newConnectionsMap[contact.connectionId] = streams[contact.connectionId]!!
                } else {
                    val keyPair = CryptoUtils.getKeyPairFromPath(contact.keyDerivationPath, mnemonicList)
                    val connection = ConnectorGRPCStream(contact.connectionId, keyPair, contact.lastMessageId)
                    connection.run(api, onMessageListener)
                    newConnectionsMap[contact.connectionId] = connection
                }
            }
            streams.clear()
            streams.putAll(newConnectionsMap)
            startReconnectionTask()
        }
    }

    /**
     * Build a coroutine that checks the connection status of each of the data streams periodically
     */
    private fun startReconnectionTask() {
        reconnectionScope?.cancel()
        reconnectionScope = CoroutineScope(Dispatchers.Default)
        reconnectionScope?.launch {
            while (true) {
                // Every X milliseconds it will check the connection status of each of the data streams
                delay(RECONNECTION_TASK_TIME_MILLIS)
                streams.forEach { (_, connection) ->
                    if (connection.isDisconnected) {
                        // if the stream is disconnected try to reconnect it
                        connection.stop()
                        connection.run(api, onMessageListener)
                    }
                }
            }
        }
    }

    private fun stopReconnectionTask() = reconnectionScope?.cancel()

    fun stopAndRemoveAllDataStreams() {
        return synchronized(this) {
            reconnectionScope?.cancel()
            streams.forEach { (_, stream) ->
                stream.stop()
            }
            streams.clear()
        }
    }
}

package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.MutableLiveData
import io.iohk.atala.prism.app.neo.common.extensions.getOrAwaitValueOrThrow
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.protos.AtalaMessage
import io.iohk.atala.prism.protos.MirrorMessage
import kotlinx.coroutines.TimeoutCancellationException

interface MirrorMessageReceiver {
    /**
     * Should handle the [MirrorMessage]´ that try to response to a previous message
     * @param atalaMessage the new message with a response
     * @param respondToMessageId the id of the message to response
     * */
    fun handleNewReceivedMessage(atalaMessage: AtalaMessage)
}

interface MirrorMessageResponseHandler {
    @Throws(TimeoutCancellationException::class)
    suspend fun awaitForResponse(messageId: String, maxTimeoutMill: Long = MirrorMessageHandler.DEFAULT_MAX_TIMEOUT_MILL): AtalaMessage?
}

class MirrorMessageHandler : MirrorMessageReceiver, MirrorMessageResponseHandler {
    companion object {
        const val DEFAULT_MAX_TIMEOUT_MILL = 10000L
        // For Singleton instantiation
        @Volatile
        private var instance: MirrorMessageHandler? = null

        fun getInstance(remoteDataSource: ConnectorRemoteDataSource): MirrorMessageHandler {
            return instance ?: synchronized(this) {
                instance ?: build(remoteDataSource).also { instance = it }
            }
        }

        private fun build(remoteDataSource: ConnectorRemoteDataSource): MirrorMessageHandler {
            val result = MirrorMessageHandler()
            result.remoteDataSource = remoteDataSource
            return result
        }
    }

    private lateinit var remoteDataSource: ConnectorRemoteDataSource

    /**
     * queue of [MutableLiveData]<[MirrorMessage]> waiting for response (the map key should be the message id)
     * */
    private val messagesNeedingResponse: MutableMap<String, MutableLiveData<AtalaMessage?>> = mutableMapOf()

    /**
     * [MirrorMessageReceiver] implementations
     * */

    override fun handleNewReceivedMessage(atalaMessage: AtalaMessage) {
        return synchronized(this) {
            messagesNeedingResponse[atalaMessage.replyTo]?.apply {
                postValue(atalaMessage) // Response a message
                messagesNeedingResponse.remove(atalaMessage.replyTo) // Remove from the stack
            }
        }
    }

    /**
     * [MirrorMessageResponseHandler] implementations
     * */

    override suspend fun awaitForResponse(messageId: String, maxTimeoutMill: Long): AtalaMessage? {
        val mutableLiveData = MutableLiveData<AtalaMessage?>()
        messagesNeedingResponse[messageId] = mutableLiveData
        return mutableLiveData.getOrAwaitValueOrThrow(maxTimeoutMill)
    }
}

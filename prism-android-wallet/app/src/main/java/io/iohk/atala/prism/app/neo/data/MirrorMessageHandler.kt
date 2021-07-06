package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.MutableLiveData
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.neo.common.extensions.getOrAwaitValueOrThrow
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.protos.CheckPayIdNameAvailabilityMessage
import io.iohk.atala.prism.protos.CheckPayIdNameAvailabilityResponse
import io.iohk.atala.prism.protos.GetPayIdNameMessage
import io.iohk.atala.prism.protos.GetPayIdNameResponse
import io.iohk.atala.prism.protos.MirrorMessage
import io.iohk.atala.prism.protos.PayIdNameRegisteredMessage
import io.iohk.atala.prism.protos.PayIdNameRegistrationMessage
import kotlinx.coroutines.TimeoutCancellationException
import java.util.concurrent.ExecutionException

interface MirrorMessageReceiver {
    /**
     * Should handle the [MirrorMessage]´ that try to response to a previous message
     * @param mirrorMessage the new message with a response
     * @param respondToMessageId the id of the message to response
     * */
    fun handleNewReceivedMessage(mirrorMessage: MirrorMessage, respondToMessageId: String)
}

interface MirrorMessageSender {
    /**
     * Send a [CheckPayIdNameAvailabilityMessage] message to a Mirror Connection and wait for a [CheckPayIdNameAvailabilityResponse] answer
     * @param payIdName name to verify
     * @param mirrorContact a mirror connection contact
     * @param maxTimeoutMill the maximum milliseconds to wait
     * @return true if available
     * */
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutCancellationException::class)
    suspend fun sendCheckPayIdNameAvailabilityMessage(
        payIdName: String,
        mirrorContact: Contact,
        maxTimeoutMill: Long = MirrorMessageHandler.DEFAULT_MAX_TIMEOUT_MILL
    ): Boolean

    /**
     * Send a [GetPayIdNameMessage] message to a Mirror Connection and wait for a [GetPayIdNameResponse] answer
     * @param mirrorContact a mirror connection contact
     * @param maxTimeoutMill the maximum milliseconds to wait
     * @return the registered Pay Id Name if it exists
     * */
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutCancellationException::class)
    suspend fun sendGetPayIdNameMessage(
        mirrorContact: Contact,
        maxTimeoutMill: Long = MirrorMessageHandler.DEFAULT_MAX_TIMEOUT_MILL
    ): String?

    /**
     * Send a [PayIdNameRegistrationMessage] message to a Mirror Connection and wait for a [PayIdNameRegisteredMessage] answer
     * @param payIdName name to register
     * @param mirrorContact a mirror connection contact
     * @param maxTimeoutMill the maximum milliseconds to wait
     * @return true if registration was successful
     * */
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutCancellationException::class)
    suspend fun sendPayIdNameRegistrationMessage(
        payIdName: String,
        mirrorContact: Contact,
        maxTimeoutMill: Long = MirrorMessageHandler.DEFAULT_MAX_TIMEOUT_MILL
    ): Boolean
}

class MirrorMessageHandler : MirrorMessageReceiver, MirrorMessageSender {
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
    private val messagesNeedingResponse: MutableMap<String, MutableLiveData<MirrorMessage?>> = mutableMapOf()

    /**
     * Send a [MirrorMessage] message to a Mirror Connection, add this message to the queue for messages needing a reply and wait for a [MirrorMessage] response
     * @param mirrorMessage message to send
     * @param mirrorContact a mirror connection contact
     * @param maxTimeoutMill the maximum milliseconds to wait
     * @return a [MirrorMessage]?
     * */
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutCancellationException::class)
    private suspend fun registerMessageForResponse(mirrorMessage: MirrorMessage, mirrorContact: Contact, maxTimeoutMill: Long): MirrorMessage? {
        val messageID = remoteDataSource.sendMirrorMessage(mirrorMessage, mirrorContact)
        val mutableLiveData = MutableLiveData<MirrorMessage?>()
        messagesNeedingResponse[messageID] = mutableLiveData
        return mutableLiveData.getOrAwaitValueOrThrow(maxTimeoutMill)
    }

    /**
     * [MirrorMessageReceiver] implementations
     * */

    override fun handleNewReceivedMessage(mirrorMessage: MirrorMessage, respondToMessageId: String) {
        return synchronized(this) {
            messagesNeedingResponse[respondToMessageId]?.apply {
                postValue(mirrorMessage) // Response a message
                messagesNeedingResponse.remove(respondToMessageId) // Remove from the stack
            }
        }
    }

    /**
     * [MirrorMessageSender] implementations
     * */

    override suspend fun sendCheckPayIdNameAvailabilityMessage(payIdName: String, mirrorContact: Contact, maxTimeoutMill: Long): Boolean {
        val message = CheckPayIdNameAvailabilityMessage.newBuilder().setNameToCheck(payIdName).build()
        val mirrorMessage = MirrorMessage.newBuilder().setCheckPayIdNameAvailabilityMessage(message).build()
        return registerMessageForResponse(
            mirrorMessage,
            mirrorContact,
            maxTimeoutMill
        )?.checkPayIdNameAvailabilityResponse?.available ?: false
    }

    override suspend fun sendGetPayIdNameMessage(mirrorContact: Contact, maxTimeoutMill: Long): String? {
        val message = GetPayIdNameMessage.newBuilder().build()
        val mirrorMessage = MirrorMessage.newBuilder().setGetPayIdNameMessage(message).build()
        return registerMessageForResponse(
            mirrorMessage,
            mirrorContact,
            maxTimeoutMill
        )?.getPayIdNameResponse?.payIdName
    }

    override suspend fun sendPayIdNameRegistrationMessage(payIdName: String, mirrorContact: Contact, maxTimeoutMill: Long): Boolean {
        val message = PayIdNameRegistrationMessage.newBuilder().setName(payIdName).build()
        val mirrorMessage = MirrorMessage.newBuilder().setPayIdNameRegistrationMessage(message).build()
        val responseMessage = registerMessageForResponse(mirrorMessage, mirrorContact, maxTimeoutMill)
        return responseMessage?.messageCase == MirrorMessage.MessageCase.PAYID_NAME_REGISTERED_MESSAGE
    }
}

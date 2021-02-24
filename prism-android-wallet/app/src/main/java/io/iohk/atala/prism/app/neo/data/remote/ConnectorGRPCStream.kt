package io.iohk.atala.prism.app.neo.data.remote

import android.util.Log
import io.grpc.Context
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.protos.GetMessageStreamResponse
import io.iohk.atala.prism.protos.ReceivedMessage

class ConnectorGRPCStream(
    val connectionId: String,
    private val keyPair: ECKeyPair,
    initialMessageId: String?
) {

    private var lastMessageId: String? = initialMessageId

    private var cancellableContexts: Context.CancellableContext? = null

    private var _isDisconnected: Boolean = true

    val isDisconnected: Boolean
        get() = _isDisconnected

    fun run(api: ConnectorRemoteDataSource, onMessageListener: OnMessageListener) {
        val runnable = Runnable {
            _isDisconnected = false
            api.startMessagesStream(
                keyPair,
                lastMessageId,
                object : StreamObserver<GetMessageStreamResponse> {
                    override fun onNext(value: GetMessageStreamResponse?) {
                        value?.message?.let { receivedMessage ->
                            onMessageListener.newMessage(receivedMessage, connectionId)
                            lastMessageId = receivedMessage.id
                        }
                    }

                    override fun onError(t: Throwable?) {
                        _isDisconnected = true
                        t?.printStackTrace()
                    }

                    override fun onCompleted() {
                        Log.i(ConnectorGRPCStream::class.simpleName, "onComplete")
                    }
                }
            )
        }
        cancellableContexts = Context.current().withCancellation()
        cancellableContexts?.run(runnable)
    }

    fun stop() {
        cancellableContexts?.cancel(null)
        cancellableContexts = null
        _isDisconnected = true
    }

    interface OnMessageListener {
        fun newMessage(message: ReceivedMessage, connectionId: String)
    }
}

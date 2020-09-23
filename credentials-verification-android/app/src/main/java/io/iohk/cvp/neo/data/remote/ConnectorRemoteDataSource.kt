package io.iohk.cvp.neo.data.remote

import io.grpc.stub.MetadataUtils
import io.iohk.atala.crypto.japi.ECKeyPair
import io.iohk.cvp.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.cvp.utils.CryptoUtils
import io.iohk.prism.protos.*

class ConnectorRemoteDataSource(sessionLocalDataSource: SessionLocalDataSourceInterface) : BaseRemoteDataSource(sessionLocalDataSource) {

    private fun getChannel(ecKeyPair: ECKeyPair?, requestByteArray: ByteArray): ConnectorServiceGrpc.ConnectorServiceFutureStub {
        val mainChannel = getMainChannel()
        var stub = ConnectorServiceGrpc.newFutureStub(mainChannel)
        if (ecKeyPair != null) {
            stub = MetadataUtils.attachHeaders(stub, CryptoUtils.getMetadata(ecKeyPair, requestByteArray))
        }
        return stub
    }

    fun getConnections(ecKeyPair: ECKeyPair): GetConnectionsPaginatedResponse {
        val request = GetConnectionsPaginatedRequest.newBuilder().setLimit(QUERY_LENGTH_LIMIT).build()
        return getChannel(ecKeyPair, request.toByteArray()).getConnectionsPaginated(request).get()
    }

    fun getAllMessages(ecKeyPair: ECKeyPair, lastMessageId: String?): GetMessagesPaginatedResponse {
        val request = GetMessagesPaginatedRequest.newBuilder().setLimit(QUERY_LENGTH_LIMIT)
        if (lastMessageId != null)
            request.lastSeenMessageId = lastMessageId
        val requestBuild = request.build()
        return getChannel(ecKeyPair, requestBuild.toByteArray()).getMessagesPaginated(requestBuild).get()
    }
}
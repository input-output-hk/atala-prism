package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;

import java.util.List;

import io.grpc.StatusRuntimeException;
import io.iohk.atala.prism.protos.ConnectionInfo;
import io.iohk.atala.prism.protos.ConnectorServiceGrpc;
import io.iohk.atala.prism.protos.GetConnectionsPaginatedRequest;
import io.iohk.atala.prism.protos.GetConnectionsPaginatedResponse;

public class GetConnectionsInfoRunnable extends CommonGrpcRunnable<List<ConnectionInfo>> {

    /*TODO remove this since it should be received as parameter when pagination its implemented
       on the fragment */
    private static final int CONNECTIONS_REQUEST_LIMIT = 10;

    public GetConnectionsInfoRunnable(
            MutableLiveData<AsyncTaskResult<List<ConnectionInfo>>> liveData) {
        super(liveData);
    }

    @Override
    public AsyncTaskResult<List<ConnectionInfo>> run(
            ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
            ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
        return getConnections(blockingStub);
    }

    private AsyncTaskResult<List<ConnectionInfo>> getConnections(
            ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub)
            throws StatusRuntimeException {

        GetConnectionsPaginatedRequest request = GetConnectionsPaginatedRequest.newBuilder()
                .setLimit(CONNECTIONS_REQUEST_LIMIT).build();

        GetConnectionsPaginatedResponse response = blockingStub.getConnectionsPaginated(request);

        return new AsyncTaskResult<>(response.getConnectionsList());
    }
}

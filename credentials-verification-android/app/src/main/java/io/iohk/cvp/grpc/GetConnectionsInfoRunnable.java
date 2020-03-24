package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectionInfo;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.GetConnectionsPaginatedRequest;
import io.iohk.cvp.io.connector.GetConnectionsPaginatedResponse;
import java.util.List;

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

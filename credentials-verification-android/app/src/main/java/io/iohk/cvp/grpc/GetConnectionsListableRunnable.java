package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.stream.Collectors;

import io.grpc.StatusRuntimeException;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import io.iohk.prism.protos.ConnectorServiceGrpc;
import io.iohk.prism.protos.GetConnectionsPaginatedRequest;
import io.iohk.prism.protos.GetConnectionsPaginatedResponse;


public class GetConnectionsListableRunnable extends CommonGrpcRunnable<List<ConnectionListable>> {

  /*TODO remove this since it should be received as parameter when pagination its implemented
     on the fragment */
  private static final int CONNECTIONS_REQUEST_LIMIT = 10;

  public GetConnectionsListableRunnable(
      MutableLiveData<AsyncTaskResult<List<ConnectionListable>>> liveData) {
    super(liveData);
  }

  @Override
  public AsyncTaskResult<List<ConnectionListable>> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return getConnections(blockingStub);
  }

  private AsyncTaskResult<List<ConnectionListable>> getConnections(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub)
      throws StatusRuntimeException {

    GetConnectionsPaginatedRequest request = GetConnectionsPaginatedRequest.newBuilder()
        .setLimit(CONNECTIONS_REQUEST_LIMIT).build();
    GetConnectionsPaginatedResponse response = blockingStub.getConnectionsPaginated(request);

    return new AsyncTaskResult<>(response.getConnectionsList().stream()
        .map(ConnectionListable::new)
        .collect(Collectors.toList()));
  }
}

package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.GetConnectionsPaginatedRequest;
import io.iohk.cvp.io.connector.GetConnectionsPaginatedResponse;
import io.iohk.cvp.io.connector.ParticipantInfo;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GetConnectionsListableRunnable extends CommonGrpcRunnable<List<ConnectionListable>> {

  /*TODO remove this since it should be received as parameter when pagination its implemented
     on the fragment */
  private static final int CONNECTIONS_REQUEST_LIMIT = 10;

  public GetConnectionsListableRunnable(MutableLiveData<List<ConnectionListable>> liveData) {
    super(liveData);
  }

  @Override
  public Optional<List<ConnectionListable>> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return getConnections(blockingStub);
  }

  private Optional<List<ConnectionListable>> getConnections(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub)
      throws StatusRuntimeException {

    GetConnectionsPaginatedRequest request = GetConnectionsPaginatedRequest.newBuilder()
        .setLimit(CONNECTIONS_REQUEST_LIMIT).build();
    GetConnectionsPaginatedResponse response = blockingStub.getConnectionsPaginated(request);

    return Optional.ofNullable(response.getConnectionsList())
        .map(connectionInfos -> connectionInfos.stream()
            .filter(
                connection -> connection.getParticipantInfo().getParticipantCase().getNumber() == ParticipantInfo.VERIFIER_FIELD_NUMBER)
            .map(ConnectionListable::new)
            .collect(Collectors.toList())
        );
  }
}

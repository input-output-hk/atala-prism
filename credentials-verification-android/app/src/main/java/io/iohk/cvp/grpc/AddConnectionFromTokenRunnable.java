package io.iohk.cvp.grpc;

import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.AddConnectionFromTokenRequest;
import io.iohk.cvp.io.connector.AddConnectionFromTokenResponse;
import io.iohk.cvp.io.connector.ConnectionInfo;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import java.util.Optional;

public class AddConnectionFromTokenRunnable implements GrpcRunnable<ConnectionInfo> {

  @Override
  public Optional<ConnectionInfo> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params)
      throws Exception {
    return addConnectionFromToken(blockingStub, params);
  }

  private Optional<ConnectionInfo> addConnectionFromToken(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub, Object... params)
      throws StatusRuntimeException {

    String token = String.valueOf(params[0]);
    AddConnectionFromTokenRequest request = AddConnectionFromTokenRequest.newBuilder()
        .setToken(token).build();
    AddConnectionFromTokenResponse response = blockingStub.addConnectionFromToken(request);

    return Optional.ofNullable(response.getConnection());
  }
}
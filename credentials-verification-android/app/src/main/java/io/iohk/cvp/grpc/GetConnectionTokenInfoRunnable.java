package io.iohk.cvp.grpc;

import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorUserServiceGrpc;
import io.iohk.cvp.io.connector.GetConnectionTokenInfoRequest;
import io.iohk.cvp.io.connector.GetConnectionTokenInfoResponse;
import io.iohk.cvp.io.connector.IssuerInfo;
import java.util.Optional;

public class GetConnectionTokenInfoRunnable implements GrpcRunnable {

  @Override
  public Optional<IssuerInfo> run(
      ConnectorUserServiceGrpc.ConnectorUserServiceBlockingStub blockingStub,
      ConnectorUserServiceGrpc.ConnectorUserServiceStub asyncStub, Object... params)
      throws Exception {
    return getConnectionToken(blockingStub, params);
  }

  private Optional<IssuerInfo> getConnectionToken(
      ConnectorUserServiceGrpc.ConnectorUserServiceBlockingStub blockingStub, Object... params)
      throws StatusRuntimeException {

    String token = String.valueOf(params[0]);
    GetConnectionTokenInfoRequest request = GetConnectionTokenInfoRequest.newBuilder()
        .setToken(token).build();
    GetConnectionTokenInfoResponse response = blockingStub.getConnectionTokenInfo(request);

    return Optional.ofNullable(response.getIssuer());
  }
}
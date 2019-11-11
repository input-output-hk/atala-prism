package io.iohk.cvp.grpc;

import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.GetConnectionTokenInfoRequest;
import io.iohk.cvp.io.connector.GetConnectionTokenInfoResponse;
import io.iohk.cvp.io.connector.ParticipantInfo;
import java.util.Optional;

public class GetConnectionTokenInfoRunnable implements GrpcRunnable<ParticipantInfo> {

  @Override
  public Optional<ParticipantInfo> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params)
      throws Exception {
    return getConnectionToken(blockingStub, params);
  }

  private Optional<ParticipantInfo> getConnectionToken(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub, Object... params)
      throws StatusRuntimeException {

    String token = String.valueOf(params[0]);
    GetConnectionTokenInfoRequest request = GetConnectionTokenInfoRequest.newBuilder()
        .setToken(token).build();
    GetConnectionTokenInfoResponse response = blockingStub.getConnectionTokenInfo(request);

    return Optional.ofNullable(response.getCreator());
  }
}

package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.GetConnectionTokenInfoRequest;
import io.iohk.cvp.io.connector.GetConnectionTokenInfoResponse;
import io.iohk.cvp.io.connector.ParticipantInfo;
import java.util.Optional;

public class GetConnectionTokenInfoRunnable extends CommonGrpcRunnable<ParticipantInfo> {

  public GetConnectionTokenInfoRunnable(MutableLiveData<ParticipantInfo> liveData) {
    super(liveData);
  }

  @Override
  public Optional<ParticipantInfo> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
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

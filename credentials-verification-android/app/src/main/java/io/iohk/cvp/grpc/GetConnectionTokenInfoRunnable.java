package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.prism.protos.ConnectorServiceGrpc;
import io.iohk.prism.protos.GetConnectionTokenInfoRequest;
import io.iohk.prism.protos.GetConnectionTokenInfoResponse;
import io.iohk.prism.protos.ParticipantInfo;

public class GetConnectionTokenInfoRunnable extends CommonGrpcRunnable<ParticipantInfo> {

  public GetConnectionTokenInfoRunnable(
      MutableLiveData<AsyncTaskResult<ParticipantInfo>> liveData) {
    super(liveData);
  }

  @Override
  public AsyncTaskResult<ParticipantInfo> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return getConnectionToken(blockingStub, params);
  }

  private AsyncTaskResult<ParticipantInfo> getConnectionToken(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub, Object... params)
      throws StatusRuntimeException {

    String token = String.valueOf(params[1]);
    GetConnectionTokenInfoRequest request = GetConnectionTokenInfoRequest.newBuilder()
        .setToken(token).build();
    GetConnectionTokenInfoResponse response = blockingStub.getConnectionTokenInfo(request);

    return new AsyncTaskResult<>(response.getCreator());
  }
}

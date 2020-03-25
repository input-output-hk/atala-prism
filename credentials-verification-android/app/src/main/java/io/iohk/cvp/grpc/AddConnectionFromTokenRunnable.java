package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;

import io.grpc.StatusRuntimeException;
import io.iohk.prism.protos.AddConnectionFromTokenRequest;
import io.iohk.prism.protos.AddConnectionFromTokenResponse;
import io.iohk.prism.protos.ConnectorPublicKey;
import io.iohk.prism.protos.ConnectorServiceGrpc;

public class AddConnectionFromTokenRunnable extends
    CommonGrpcRunnable<AddConnectionFromTokenResponse> {

  public AddConnectionFromTokenRunnable(
      MutableLiveData<AsyncTaskResult<AddConnectionFromTokenResponse>> liveData) {
    super(liveData);
  }

  @Override
  public AsyncTaskResult<AddConnectionFromTokenResponse> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return addConnectionFromToken(blockingStub, params);
  }

  private AsyncTaskResult<AddConnectionFromTokenResponse> addConnectionFromToken(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub, Object... params)
      throws StatusRuntimeException {

    String token = String.valueOf(params[1]);
    ConnectorPublicKey publicKey = (ConnectorPublicKey) params[2];
    String nonce = String.valueOf(params[3]);

    AddConnectionFromTokenRequest request = AddConnectionFromTokenRequest.newBuilder()
        .setToken(token)
        .setPaymentNonce(nonce)
        .setHolderPublicKey(publicKey).build();
    AddConnectionFromTokenResponse response = blockingStub.addConnectionFromToken(request);

    return new AsyncTaskResult<>(response);
  }
}
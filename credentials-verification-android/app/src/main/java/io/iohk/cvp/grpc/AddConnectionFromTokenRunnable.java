package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.AddConnectionFromTokenRequest;
import io.iohk.cvp.io.connector.AddConnectionFromTokenResponse;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.PublicKey;
import java.util.Optional;

public class AddConnectionFromTokenRunnable extends
    CommonGrpcRunnable<AddConnectionFromTokenResponse> {

  public AddConnectionFromTokenRunnable(MutableLiveData<AddConnectionFromTokenResponse> liveData) {
    super(liveData);
  }

  @Override
  public Optional<AddConnectionFromTokenResponse> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return addConnectionFromToken(blockingStub, params);
  }

  private Optional<AddConnectionFromTokenResponse> addConnectionFromToken(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub, Object... params)
      throws StatusRuntimeException {

    String token = String.valueOf(params[1]);
    PublicKey publicKey = (PublicKey) params[2];

    AddConnectionFromTokenRequest request = AddConnectionFromTokenRequest.newBuilder()
        .setToken(token)
        .setHolderPublicKey(publicKey).build();
    AddConnectionFromTokenResponse response = blockingStub.addConnectionFromToken(request);

    return Optional.ofNullable(response);
  }
}
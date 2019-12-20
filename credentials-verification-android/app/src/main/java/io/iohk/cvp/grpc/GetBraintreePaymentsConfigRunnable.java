package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc.ConnectorServiceBlockingStub;
import io.iohk.cvp.io.connector.GetBraintreePaymentsConfigRequest;
import io.iohk.cvp.io.connector.GetBraintreePaymentsConfigResponse;
import java.util.Optional;

public class GetBraintreePaymentsConfigRunnable extends CommonGrpcRunnable<String> {

  public GetBraintreePaymentsConfigRunnable(MutableLiveData<String> liveData) {
    super(liveData);
  }

  @Override
  public Optional<String> run(
      ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return getTokenizationKey(blockingStub);
  }

  private Optional<String> getTokenizationKey(
      ConnectorServiceBlockingStub blockingStub)
      throws StatusRuntimeException {

    GetBraintreePaymentsConfigRequest request = GetBraintreePaymentsConfigRequest.newBuilder()
        .build();
    GetBraintreePaymentsConfigResponse response = blockingStub.getBraintreePaymentsConfig(request);

    return Optional.of(response.getTokenizationKey());
  }

}

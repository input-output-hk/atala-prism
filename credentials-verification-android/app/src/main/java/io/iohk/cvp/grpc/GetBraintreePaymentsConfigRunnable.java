package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;

import io.grpc.StatusRuntimeException;
import io.iohk.prism.protos.ConnectorServiceGrpc.ConnectorServiceBlockingStub;
import io.iohk.prism.protos.ConnectorServiceGrpc.ConnectorServiceStub;
import io.iohk.prism.protos.GetBraintreePaymentsConfigRequest;
import io.iohk.prism.protos.GetBraintreePaymentsConfigResponse;

public class GetBraintreePaymentsConfigRunnable extends CommonGrpcRunnable<String> {

  public GetBraintreePaymentsConfigRunnable(MutableLiveData<AsyncTaskResult<String>> liveData) {
    super(liveData);
  }

  @Override
  public AsyncTaskResult<String> run(
      ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceStub asyncStub, Object... params) {
    return getTokenizationKey(blockingStub);
  }

  private AsyncTaskResult<String> getTokenizationKey(
      ConnectorServiceBlockingStub blockingStub)
      throws StatusRuntimeException {

    GetBraintreePaymentsConfigRequest request = GetBraintreePaymentsConfigRequest.newBuilder()
        .build();
    GetBraintreePaymentsConfigResponse response = blockingStub.getBraintreePaymentsConfig(request);

    return new AsyncTaskResult<>(response.getTokenizationKey());
  }

}

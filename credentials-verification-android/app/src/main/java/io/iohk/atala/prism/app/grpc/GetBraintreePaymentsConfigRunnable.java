package io.iohk.atala.prism.app.grpc;

import androidx.lifecycle.MutableLiveData;

import io.grpc.StatusRuntimeException;
import io.iohk.atala.prism.protos.ConnectorServiceGrpc;
import io.iohk.atala.prism.protos.GetBraintreePaymentsConfigRequest;
import io.iohk.atala.prism.protos.GetBraintreePaymentsConfigResponse;

public class GetBraintreePaymentsConfigRunnable extends CommonGrpcRunnable<String> {

    public GetBraintreePaymentsConfigRunnable(MutableLiveData<AsyncTaskResult<String>> liveData) {
        super(liveData);
    }

    @Override
    public AsyncTaskResult<String> run(
            ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
            ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
        return getTokenizationKey(blockingStub);
    }

    private AsyncTaskResult<String> getTokenizationKey(
            ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub)
            throws StatusRuntimeException {
        GetBraintreePaymentsConfigRequest request = GetBraintreePaymentsConfigRequest.newBuilder()
                .build();
        GetBraintreePaymentsConfigResponse response = blockingStub.getBraintreePaymentsConfig(request);

        return new AsyncTaskResult<>(response.getTokenizationKey());
    }

}

package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;

import io.grpc.StatusRuntimeException;
import io.iohk.prism.protos.ConnectorServiceGrpc;
import io.iohk.prism.protos.ProcessPaymentRequest;
import io.iohk.prism.protos.ProcessPaymentResponse;

public class ProcessPaymentRunnable extends CommonGrpcRunnable<ProcessPaymentResponse> {

    public ProcessPaymentRunnable(MutableLiveData<AsyncTaskResult<ProcessPaymentResponse>> liveData) {
        super(liveData);
    }

    @Override
    public AsyncTaskResult<ProcessPaymentResponse> run(
            ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
            ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
        return processPayment(blockingStub, params);
    }

    private AsyncTaskResult<ProcessPaymentResponse> processPayment(
            ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub, Object... params)
            throws StatusRuntimeException {
        ProcessPaymentRequest request = ProcessPaymentRequest.newBuilder()
                .setAmount(String.valueOf(params[1])).setNonce(String.valueOf(params[2])).build();
        ProcessPaymentResponse response = blockingStub.processPayment(request);

        return new AsyncTaskResult<>(response);
    }

}

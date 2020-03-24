package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc.ConnectorServiceBlockingStub;
import io.iohk.cvp.io.connector.ProcessPaymentRequest;
import io.iohk.cvp.io.connector.ProcessPaymentResponse;

public class ProcessPaymentRunnable extends CommonGrpcRunnable<ProcessPaymentResponse> {

  public ProcessPaymentRunnable(MutableLiveData<AsyncTaskResult<ProcessPaymentResponse>> liveData) {
    super(liveData);
  }

  @Override
  public AsyncTaskResult<ProcessPaymentResponse> run(
      ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return processPayment(blockingStub, params);
  }

  private AsyncTaskResult<ProcessPaymentResponse> processPayment(
      ConnectorServiceBlockingStub blockingStub, Object... params)
      throws StatusRuntimeException {

    ProcessPaymentRequest request = ProcessPaymentRequest.newBuilder()
        .setAmount(String.valueOf(params[1])).setNonce(String.valueOf(params[2])).build();
    ProcessPaymentResponse response = blockingStub.processPayment(request);

    return new AsyncTaskResult<>(response);
  }

}

package io.iohk.atala.prism.app.grpc;

import androidx.lifecycle.MutableLiveData;

import java.util.List;

import io.grpc.StatusRuntimeException;
import io.iohk.atala.prism.protos.ConnectorServiceGrpc;
import io.iohk.atala.prism.protos.GetPaymentsRequest;
import io.iohk.atala.prism.protos.GetPaymentsResponse;
import io.iohk.atala.prism.protos.Payment;

public class GetPaymentsRunnable extends CommonGrpcRunnable<List<Payment>> {

  public GetPaymentsRunnable(MutableLiveData<AsyncTaskResult<List<Payment>>> liveData) {
    super(liveData);
  }

  @Override
  public AsyncTaskResult<List<Payment>> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return getPayments(blockingStub);
  }

  private AsyncTaskResult<List<Payment>> getPayments(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub)
      throws StatusRuntimeException {

    GetPaymentsRequest request = GetPaymentsRequest.newBuilder().build();
    GetPaymentsResponse response = blockingStub.getPayments(request);

    return new AsyncTaskResult<>(response.getPaymentsList());
  }
}

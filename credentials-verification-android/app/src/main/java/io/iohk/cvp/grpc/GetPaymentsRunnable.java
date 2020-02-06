package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.GetPaymentsRequest;
import io.iohk.cvp.io.connector.GetPaymentsResponse;
import io.iohk.cvp.io.connector.Payment;
import java.util.List;

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

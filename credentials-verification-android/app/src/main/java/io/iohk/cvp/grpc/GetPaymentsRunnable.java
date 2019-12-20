package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.GetPaymentsRequest;
import io.iohk.cvp.io.connector.GetPaymentsResponse;
import io.iohk.cvp.io.connector.Payment;
import java.util.List;
import java.util.Optional;

public class GetPaymentsRunnable extends CommonGrpcRunnable<List<Payment>> {

  public GetPaymentsRunnable(MutableLiveData<List<Payment>> liveData) {
    super(liveData);
  }

  @Override
  public Optional<List<Payment>> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return getPayments(blockingStub);
  }

  private Optional<List<Payment>> getPayments(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub)
      throws StatusRuntimeException {

    GetPaymentsRequest request = GetPaymentsRequest.newBuilder().build();
    GetPaymentsResponse response = blockingStub.getPayments(request);

    return Optional.ofNullable(response.getPaymentsList());
  }
}

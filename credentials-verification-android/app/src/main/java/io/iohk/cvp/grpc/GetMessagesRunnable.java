package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc.ConnectorServiceBlockingStub;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc.ConnectorServiceStub;
import io.iohk.cvp.io.connector.GetMessagesPaginatedRequest;
import io.iohk.cvp.io.connector.GetMessagesPaginatedResponse;
import io.iohk.cvp.io.connector.ReceivedMessage;
import java.util.List;

public class GetMessagesRunnable<A> extends
    CommonGrpcRunnable<List<ReceivedMessage>> {

  // FIXME this is hardcoded since we are not going to implement pagination for alpha
  private static final int QUERY_LENGTH = 100;

  public GetMessagesRunnable(
      MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> liveData) {
    super(liveData);
  }

  @Override
  public AsyncTaskResult<List<ReceivedMessage>> run(
      ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceStub asyncStub, Object... params) {
    return getMessages(blockingStub);
  }

  private AsyncTaskResult<List<ReceivedMessage>> getMessages(
      ConnectorServiceBlockingStub blockingStub)
      throws StatusRuntimeException {

    GetMessagesPaginatedRequest request = GetMessagesPaginatedRequest.newBuilder()
        .setLimit(QUERY_LENGTH).build();
    GetMessagesPaginatedResponse response = blockingStub.getMessagesPaginated(request);

    return new AsyncTaskResult<>(response.getMessagesList());
  }

}

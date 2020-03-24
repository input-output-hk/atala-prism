package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.prism.protos.ConnectorServiceGrpc;
import io.iohk.prism.protos.GetMessagesPaginatedRequest;
import io.iohk.prism.protos.GetMessagesPaginatedResponse;
import io.iohk.prism.protos.ReceivedMessage;

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
          ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
          ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return getMessages(blockingStub);
  }

  private AsyncTaskResult<List<ReceivedMessage>> getMessages(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub)
      throws StatusRuntimeException {

    GetMessagesPaginatedRequest request = GetMessagesPaginatedRequest.newBuilder()
        .setLimit(QUERY_LENGTH).build();
    GetMessagesPaginatedResponse response = blockingStub.getMessagesPaginated(request);

    return new AsyncTaskResult<>(response.getMessagesList());
  }

}

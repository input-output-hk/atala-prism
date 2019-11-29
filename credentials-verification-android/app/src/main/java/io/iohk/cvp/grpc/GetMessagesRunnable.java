package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc.ConnectorServiceBlockingStub;
import io.iohk.cvp.io.connector.GetMessagesPaginatedRequest;
import io.iohk.cvp.io.connector.GetMessagesPaginatedResponse;
import io.iohk.cvp.io.connector.ReceivedMessage;
import java.util.List;
import java.util.Optional;

public class GetMessagesRunnable extends CommonGrpcRunnable<List<ReceivedMessage>> {

  // FIXME this is hardcoded since we are not going to implement pagination for alpha
  private static final int QUERY_LENGTH = 100;

  public GetMessagesRunnable(MutableLiveData<List<ReceivedMessage>> liveData) {
    super(liveData);
  }

  @Override
  public Optional<List<ReceivedMessage>> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return getMessages(blockingStub);
  }

  private Optional<List<ReceivedMessage>> getMessages(
      ConnectorServiceBlockingStub blockingStub)
      throws StatusRuntimeException {

    GetMessagesPaginatedRequest request = GetMessagesPaginatedRequest.newBuilder()
        .setLimit(QUERY_LENGTH).build();
    GetMessagesPaginatedResponse response = blockingStub.getMessagesPaginated(request);

    return Optional.of(response.getMessagesList());
  }

}

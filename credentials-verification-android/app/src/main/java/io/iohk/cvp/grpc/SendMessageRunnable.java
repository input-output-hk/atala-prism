package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.SendMessageRequest;
import io.iohk.cvp.io.connector.SendMessageResponse;
import java.util.Optional;

public class SendMessageRunnable extends CommonGrpcRunnable<Boolean> {

  public SendMessageRunnable(MutableLiveData<Boolean> liveData) {
    super(liveData);
  }

  @Override
  public Optional<Boolean> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return sendMessage(blockingStub, params);
  }

  private Optional<Boolean> sendMessage(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub, Object... params)
      throws StatusRuntimeException {

    String connectionId = (String) params[1];
    ByteString message = (ByteString) params[2];

    SendMessageRequest request = SendMessageRequest.newBuilder()
        .setConnectionId(connectionId)
        .setMessage(message)
        .build();

    SendMessageResponse response = blockingStub.sendMessage(request);

    return Optional.of(response.isInitialized());
  }
}

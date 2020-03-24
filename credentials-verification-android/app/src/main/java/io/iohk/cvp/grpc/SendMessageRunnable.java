package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import io.iohk.prism.protos.ConnectorServiceGrpc;
import io.iohk.prism.protos.SendMessageRequest;
import io.iohk.prism.protos.SendMessageResponse;

public class SendMessageRunnable extends CommonGrpcRunnable<Boolean> {

  public SendMessageRunnable(MutableLiveData<AsyncTaskResult<Boolean>> liveData) {
    super(liveData);
  }

  @Override
  public AsyncTaskResult<Boolean> run(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params) {
    return sendMessage(blockingStub, params);
  }

  private AsyncTaskResult<Boolean> sendMessage(
      ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub, Object... params)
      throws StatusRuntimeException {

    String connectionId = (String) params[1];
    ByteString message = (ByteString) params[2];

    SendMessageRequest request = SendMessageRequest.newBuilder()
        .setConnectionId(connectionId)
        .setMessage(message)
        .build();

    SendMessageResponse response = blockingStub.sendMessage(request);

    return new AsyncTaskResult<>(response.isInitialized());
  }
}

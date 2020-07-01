package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;

import com.google.protobuf.ByteString;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

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
    List<ByteString> messages = (List<ByteString>) params[2];

    for (ByteString message:messages) {
      SendMessageRequest request = SendMessageRequest.newBuilder()
              .setConnectionId(connectionId)
              .setMessage(message)
              .build();
      try {
        blockingStub.sendMessage(request);
      } catch (Exception ex) {
        return new AsyncTaskResult<>(ex);
      }
    }
    return new AsyncTaskResult<>(true);
  }
}

package io.iohk.cvp.grpc;

import android.os.AsyncTask;
import io.grpc.ManagedChannel;
import io.iohk.cvp.io.connector.ConnectorUserServiceGrpc;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

public class GrpcTask extends AsyncTask<Object, Void, Optional<?>> {

  private final GrpcRunnable grpcRunnable;
  private final ManagedChannel channel;

  public GrpcTask(GrpcRunnable grpcRunnable, ManagedChannel channel) {
    this.grpcRunnable = grpcRunnable;
    this.channel = channel;
  }

  @Override
  public Optional<?> doInBackground(Object... params) {
    try {
      return
          grpcRunnable.run(
              ConnectorUserServiceGrpc.newBlockingStub(channel),
              ConnectorUserServiceGrpc.newStub(channel), params);
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      pw.flush();
      // TODO handle error
      return Optional.empty();
    }
  }
}

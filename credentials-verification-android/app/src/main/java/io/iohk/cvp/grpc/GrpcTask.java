package io.iohk.cvp.grpc;

import android.os.AsyncTask;
import com.crashlytics.android.Crashlytics;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc.ConnectorServiceBlockingStub;
import io.iohk.cvp.io.connector.ConnectorServiceGrpc.ConnectorServiceStub;
import java.util.Optional;

public class GrpcTask<A> extends AsyncTask<Object, Void, Optional<A>> {

  private final GrpcRunnable<A> grpcRunnable;
  private final ManagedChannel origChannel;

  public GrpcTask(GrpcRunnable<A> grpcRunnable) {
    this.grpcRunnable = grpcRunnable;
    this.origChannel = ManagedChannelBuilder
        .forAddress(BuildConfig.API_BASE_URL, BuildConfig.API_PORT).usePlaintext()
        .build();
  }

  @Override
  public Optional<A> doInBackground(Object... params) {
    try {
      ClientInterceptor interceptor = new HeaderClientInterceptor();
      Channel channel = ClientInterceptors.intercept(origChannel, interceptor);
      ConnectorServiceBlockingStub
          blockingStub = ConnectorServiceGrpc.newBlockingStub(channel);
      ConnectorServiceStub stub = ConnectorServiceGrpc.newStub(channel);

      return
          grpcRunnable.run(blockingStub, stub, params);
    } catch (Exception e) {
      Crashlytics.logException(e);
      // TODO handle error
      return Optional.empty();
    }
  }
}

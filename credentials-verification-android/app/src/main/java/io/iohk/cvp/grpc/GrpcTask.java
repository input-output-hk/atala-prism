package io.iohk.cvp.grpc;

import android.content.Context;
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
import io.iohk.cvp.views.Preferences;
import java.util.Optional;

public class GrpcTask<A> extends AsyncTask<Object, Void, Optional<A>> {

  private final GrpcRunnable<A> grpcRunnable;
  private final ManagedChannel origChannel;

  public GrpcTask(GrpcRunnable<A> grpcRunnable, Context context) {
    Preferences prefs = new Preferences(context);
    String ip = prefs.getString(Preferences.BACKEND_IP);
    Integer port = prefs.getInt(Preferences.BACKEND_PORT);

    this.origChannel = ManagedChannelBuilder
        .forAddress(ip.equals("") ? BuildConfig.API_BASE_URL : ip,
            port.equals(0) ? BuildConfig.API_PORT : port)
        .usePlaintext()
        .build();
    this.grpcRunnable = grpcRunnable;

  }

  @Override
  public Optional<A> doInBackground(Object... params) {
    try {
      String userId = getUserId(params);
      ClientInterceptor interceptor = new HeaderClientInterceptor(userId);
      Channel channel = ClientInterceptors.intercept(origChannel, interceptor);
      ConnectorServiceBlockingStub
          blockingStub = ConnectorServiceGrpc.newBlockingStub(channel);
      ConnectorServiceStub stub = ConnectorServiceGrpc.newStub(channel);

      return grpcRunnable.run(blockingStub, stub, params);
    } catch (Exception e) {
      Crashlytics.logException(e);
      // TODO handle error
      return Optional.empty();
    }
  }

  // FIXME we should find a better way to send user id
  private String getUserId(Object[] params) {
    if (params.length == 0 || params[0] == null) {
      return null;
    }
    return String.valueOf(params[0]);
  }

  @Override
  protected void onPostExecute(final Optional<A> a) {
    super.onPostExecute(a);
    a.ifPresent(grpcRunnable::onPostExecute);
  }
}

package io.iohk.atala.prism.app.grpc;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.iohk.cvp.BuildConfig;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;
import io.iohk.atala.prism.protos.ConnectorServiceGrpc;

public class GrpcTask<A> extends AsyncTask<Object, Void, AsyncTaskResult<A>> {

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
    public AsyncTaskResult<A> doInBackground(Object... params) {
        Set<String> userIds = getUserIds(params);
        List<A> returnList = new ArrayList<>();
        if (userIds != null) {
            userIds.forEach(userId -> {
                ClientInterceptor interceptor = new HeaderClientInterceptor(userId);
                Channel channel = ClientInterceptors.intercept(origChannel, interceptor);
                ConnectorServiceGrpc.ConnectorServiceBlockingStub
                        blockingStub = ConnectorServiceGrpc.newBlockingStub(channel);
                ConnectorServiceGrpc.ConnectorServiceStub stub = ConnectorServiceGrpc.newStub(channel);
                try {
                    A result = grpcRunnable.run(blockingStub, stub, params).getResult();
                    if (result instanceof List) {
                        returnList.addAll((List) result);
                    } else {
                        returnList.add(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return new AsyncTaskResult(returnList);
        } else {
            String userId = getUserId(params);

            ClientInterceptor interceptor = new HeaderClientInterceptor(userId);
            Channel channel = ClientInterceptors.intercept(origChannel, interceptor);
            ConnectorServiceGrpc.ConnectorServiceBlockingStub
                    blockingStub = ConnectorServiceGrpc.newBlockingStub(channel);
            ConnectorServiceGrpc.ConnectorServiceStub stub = ConnectorServiceGrpc.newStub(channel);

            try {
                return grpcRunnable.run(blockingStub, stub, params);
            } catch (Exception e) {
                return new AsyncTaskResult<>(e);
            }

        }
    }

    // FIXME we should find a better way to send user id
    private String getUserId(Object[] params) {
        if (params.length == 0 || params[0] == null) {
            return null;
        }
        return String.valueOf(params[0]);
    }

    private Set<String> getUserIds(Object[] params) {
        try {
            if (params == null || (params.length == 0 || params[0] == null)) {
                return null;
            }
            return (Set<String>) params[0];
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(final AsyncTaskResult<A> a) {
        super.onPostExecute(a);
        grpcRunnable.onPostExecute(a);
    }
}
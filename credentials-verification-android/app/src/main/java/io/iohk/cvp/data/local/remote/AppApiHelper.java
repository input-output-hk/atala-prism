package io.iohk.cvp.data.local.remote;

import android.content.Context;

import javax.inject.Inject;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.core.CvpApplication;
import io.iohk.cvp.views.Preferences;
import io.iohk.prism.protos.ConnectorServiceGrpc;

public class AppApiHelper implements ApiHelper {

    private final Context context;
    private ManagedChannel origChannel;
    private Metadata.Key<String> HEADER_USER_ID_KEY =
            Metadata.Key.of("userId", Metadata.ASCII_STRING_MARSHALLER);

    @Inject
    public AppApiHelper(CvpApplication cvpApplication) {
        this.context = cvpApplication.getApplicationContext();
        initChannel();
    }

    private void initChannel() {
        Preferences prefs = new Preferences(context);
        String ip = prefs.getString(Preferences.BACKEND_IP);
        Integer port = prefs.getInt(Preferences.BACKEND_PORT);

        origChannel = ManagedChannelBuilder
                .forAddress(ip.equals("") ? BuildConfig.API_BASE_URL : ip,
                        port.equals(0) ? BuildConfig.API_PORT : port)
                .usePlaintext()
                .build();
    }

    public ConnectorServiceGrpc.ConnectorServiceFutureStub getChannel(String userId) {
        ConnectorServiceGrpc.ConnectorServiceFutureStub stub = ConnectorServiceGrpc.newFutureStub(origChannel);
        Metadata m = new Metadata();
        m.put(HEADER_USER_ID_KEY, userId);
        stub = MetadataUtils.attachHeaders(stub, m);
        return stub;
    }

}

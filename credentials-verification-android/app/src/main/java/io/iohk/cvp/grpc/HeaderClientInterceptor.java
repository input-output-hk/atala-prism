package io.iohk.cvp.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.util.logging.Logger;

public class HeaderClientInterceptor implements ClientInterceptor {

  private static final Logger logger = Logger.getLogger(HeaderClientInterceptor.class.getName());

  static final Metadata.Key<String> HEADER_USER_ID_KEY =
      Metadata.Key.of("userId", Metadata.ASCII_STRING_MARSHALLER);

  private final String userId;

  public HeaderClientInterceptor(String userId) {
    this.userId = userId;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
      CallOptions callOptions, Channel next) {
    return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        if (userId != null) {
          headers.put(HEADER_USER_ID_KEY, userId);
        } else { // FIXME remove else condition once server supports request without user id
          headers.put(HEADER_USER_ID_KEY, "600c93de-2843-4391-a3fc-8078bbe2f8fc");
        }
        super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
          @Override
          public void onHeaders(Metadata headers) {
            super.onHeaders(headers);
          }
        }, headers);
      }
    };
  }
}
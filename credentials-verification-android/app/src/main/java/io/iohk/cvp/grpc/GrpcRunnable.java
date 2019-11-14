package io.iohk.cvp.grpc;

import io.iohk.cvp.io.connector.ConnectorServiceGrpc;
import java.util.Optional;

public interface GrpcRunnable<A> {

  /**
   * Perform a grpcRunnable and return all the logs.
   */
  Optional<A> run(ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub,
      ConnectorServiceGrpc.ConnectorServiceStub asyncStub, Object... params)
      throws Exception;

  void onPostExecute(A result);

}

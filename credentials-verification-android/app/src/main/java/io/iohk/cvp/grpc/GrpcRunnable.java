package io.iohk.cvp.grpc;

import io.iohk.cvp.io.connector.ConnectorUserServiceGrpc;
import java.util.Optional;

public interface GrpcRunnable {

  /**
   * Perform a grpcRunnable and return all the logs.
   */
  Optional<?> run(ConnectorUserServiceGrpc.ConnectorUserServiceBlockingStub blockingStub,
      ConnectorUserServiceGrpc.ConnectorUserServiceStub asyncStub, Object... params)
      throws Exception;
}
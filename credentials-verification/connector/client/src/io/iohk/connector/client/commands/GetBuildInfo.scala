package io.iohk.atala.prism.connector.client.commands

import io.iohk.atala.prism.connector.client.Config
import io.iohk.atala.prism.protos.connector_api.ConnectorServiceGrpc
import io.iohk.atala.prism.protos.connector_api

case class GetBuildInfo() extends Command {
  override def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit = {
    val response = api.getBuildInfo(connector_api.GetBuildInfoRequest())
    print(response.toProtoString)
  }
}

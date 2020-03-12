package io.iohk.connector.client.commands

import io.iohk.connector.client.Config
import io.iohk.prism.protos.connector_api.ConnectorServiceGrpc

case object UnitCommand extends Command {
  override def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit = ()
}

package io.iohk.atala.prism.connector.client.commands

import io.iohk.atala.prism.connector.client.Config
import io.iohk.atala.prism.protos.connector_api.ConnectorServiceGrpc

case object UnitCommand extends Command {
  override def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit = ()
}

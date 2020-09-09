package io.iohk.atala.prism.node.client.commands

import io.iohk.atala.prism.node.client.Config
import io.iohk.prism.protos.node_api

case class GetBuildInfo() extends Command {
  override def run(api: node_api.NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit = {
    val response = api.getNodeBuildInfo(node_api.GetNodeBuildInfoRequest())
    print(response.toProtoString)
  }
}

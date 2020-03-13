package io.iohk.node.client.commands

import io.iohk.node.client.Config
import io.iohk.prism.protos.node_api

case class GetBuildInfo() extends Command {
  override def run(api: node_api.NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit = {
    val response = api.getBuildInfo(node_api.GetBuildInfoRequest())
    print(response.toProtoString)
  }
}

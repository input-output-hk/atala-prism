package io.iohk.node.client.commands

import io.iohk.node.client.Config
import io.iohk.prism.protos.node_api
import monocle.Optional
import monocle.macros.{GenLens, GenPrism}
import monocle.std.option.some
import scopt.OParser

case class Resolve(did: String = "") extends Command {
  override def run(api: node_api.NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit = {
    val response = api.getDidDocument(node_api.GetDidDocumentRequest(did = did))
    print(response.document.get.toProtoString)
  }
}

object Resolve {
  import Config._
  val lens: Optional[Config, Resolve] =
    GenLens[Config](_.command).composePrism(some).composePrism(GenPrism[Command, Resolve])

  val parser = {
    import parserBuilder._

    OParser.sequence(
      arg[String]("<did>").action(lens.composeLens(GenLens[Resolve](_.did)).optify)
    )
  }
}

package io.iohk.connector.client

import io.grpc.ManagedChannelBuilder
import io.iohk.connector.client.commands.Register
import io.iohk.prism.protos.connector_api.ConnectorServiceGrpc
import scopt.OParser

object ConnectorClient {

  private val parser = {
    import Config.parserBuilder._

    OParser.sequence(
      programName("ConnectorClient"),
      opt[String]('h', "host")
        .valueName("<host>")
        .action((x, c) => c.copy(host = x)),
      opt[Int]('p', "port")
        .valueName("<port>")
        .action((x, c) => c.copy(port = x)),
      cmd("register")
        .action((x, c) => c.copy(command = Some(Register())))
    )
  }

  def main(args: Array[String]): Unit = {
    val configOpt = OParser.parse(parser, args, Config())

    configOpt match {
      case Some(config @ Config(Some(command), host, port)) =>
        val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
        val api = ConnectorServiceGrpc.blockingStub(channel)
        command.run(api, config)

      case Some(Config(None, _, _)) =>
        println(OParser.usage(parser))

      case None => ()
    }
  }
}

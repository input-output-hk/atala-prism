package io.iohk.connector.client

import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc._
import io.iohk.connector.client.commands._
import io.iohk.prism.protos.connector_api.ConnectorServiceGrpc
import scopt.OParser

object ConnectorClient {

  import ch.qos.logback.classic.util.ContextInitializer

  System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "/logback.xml")

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
      opt[String]('u', "userId")
        .action((userId, c) => c.copy(userId = Some(userId))),
      cmd("register")
        .action((x, c) => c.copy(command = Register())),
      cmd("accept-connection")
        .action((_, c) => c.copy(command = AcceptConnection))
        .children(
          opt[String]("token")
            .required()
            .action((token, c) => c.copy(connectionToken = Some(token)))
            .text("The connection token to accept"),
          opt[String]('x', "pub-key-x")
            .required()
            .action((x, c) => c.copy(pubKeyX = Some(x))),
          opt[String]('y', "pub-key-y")
            .required()
            .action((y, c) => c.copy(pubKeyY = Some(y)))
        ),
      cmd("get-connection")
        .children(
          opt[String]("token")
            .required()
            .action((token, c) => c.copy(connectionToken = Some(token), command = GetConnection))
        ),
      cmd("get-messages")
        .children(
          opt[String]("token")
            .required()
            .action((token, c) => c.copy(connectionToken = Some(token), command = GetMessages))
        ),
      cmd("send-message")
        .action((_, c) => c.copy(command = SendMessage))
        .children(
          opt[String]("token")
            .required()
            .action((token, c) => c.copy(connectionToken = Some(token))),
          opt[String]("base64-message")
            .text("Base64 encoded message")
            .required()
            .action((message, c) => c.copy(base64Message = Some(message)))
        ),
      cmd("get-build-info")
        .action((x, c) => c.copy(command = GetBuildInfo()))
    )
  }

  def main(args: Array[String]): Unit = {
    val configOpt = OParser.parse(parser, args, Config())

    configOpt match {
      case Some(config) =>
        if (config.command == UnitCommand) {
          OParser.usage(parser)
          ()
        } else {
          val api = ConnectorServiceGrpc.blockingStub(getChannel(config))
          config.command.run(api, config)
        }
      case None => ()
    }
  }

  class UserIdInterceptor(userId: String) extends ClientInterceptor {
    private val userIdHeader = Metadata.Key.of("userId", Metadata.ASCII_STRING_MARSHALLER)
    override def interceptCall[ReqT, RespT](
        method: MethodDescriptor[ReqT, RespT],
        callOptions: CallOptions,
        next: Channel
    ): ClientCall[ReqT, RespT] = {
      new SimpleForwardingClientCall[ReqT, RespT](next.newCall(method, callOptions)) {
        override def start(responseListener: ClientCall.Listener[RespT], headers: Metadata): Unit = {
          headers.put(userIdHeader, userId)
          super.start(responseListener, headers)
        }
      }
    }
  }

  private def getChannel(config: Config): Channel = {
    val unauthenticatedChannel = ManagedChannelBuilder
      .forAddress(config.host, config.port)
      .usePlaintext()
      .build()

    config.userId
      .fold[Channel](unauthenticatedChannel)(userId =>
        ClientInterceptors.intercept(unauthenticatedChannel, new UserIdInterceptor(userId))
      )
  }
}

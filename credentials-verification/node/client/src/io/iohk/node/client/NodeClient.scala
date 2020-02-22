package io.iohk.node.client

import java.io.File

import io.grpc._
import io.iohk.node.client.commands._
import io.iohk.node.geud_node._
import monocle.POptional
import scopt.OParser

object Config {
  val parserBuilder = OParser.builder[Config]

  /** Util to use monocle Optional optics to update fields in scopt config
    *
    * Scalaopt options require providing function (Value, Config) => Config to apply user provided value
    * to the config. This is quite mundane, especially for nested values. Lenses / other optics
    * come to help.
    *
    * Optional is an optics that points to a possible field. Reading from it returns Option[T]
    * where T is the underlying type, while writing modifies the field iff it is present.
    */
  implicit class OptifyHelper[Config, Field](optional: POptional[Config, Config, Field, Field]) {

    /** Sets the field to value provided by user */
    def optify: (Field, Config) => Config = { (value: Field, c: Config) =>
      optional.set(value)(c)
    }

    /** Sets the field to transformation of value provided by user */
    def optify[T](f: T => Field): (T, Config) => Config = { (value: T, c: Config) =>
      optional.set(f(value))(c)
    }
  }
}
case class Config(
    command: Option[Command] = None,
    host: String = "localhost",
    port: Int = 50053,
    stateStorage: File = StateStorage.DEFAULT_STORAGE
)

object NodeClient {

  val parser = {
    import Config.parserBuilder._

    OParser.sequence(
      programName("NodeClient"),
      opt[String]('h', "host")
        .valueName("<host>")
        .action((x, c) => c.copy(host = x)),
      opt[Int]('p', "port")
        .valueName("<port>")
        .action((x, c) => c.copy(port = x)),
      opt[String]('s', "storage")
        .valueName("<key-storage-path>")
        .action((x, c) => c.copy(stateStorage = new File(x))),
      cmd("resolve")
        .action((x, c) => c.copy(command = Some(Resolve())))
        .children(Resolve.parser),
      cmd("create-did")
        .action((x, c) => c.copy(command = Some(CreateDid())))
        .children(CreateDid.parser),
      cmd("update-did")
        .action((x, c) => c.copy(command = Some(UpdateDid())))
        .children(UpdateDid.parser),
      cmd("issue-credential")
        .action((x, c) => c.copy(command = Some(IssueCredential())))
        .children(IssueCredential.parser),
      cmd("revoke-credential")
        .action((x, c) => c.copy(command = Some(RevokeCredential())))
        .children(RevokeCredential.parser)
    )
  }

  def main(args: Array[String]): Unit = {
    val configOpt = OParser.parse(parser, args, Config())

    configOpt match {
      case Some(config @ Config(Some(command), host, port, _)) =>
        val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
        val api = NodeServiceGrpc.blockingStub(channel)
        command.run(api, config)
      case Some(Config(None, _, _, _)) =>
        println(OParser.usage(parser))
      case None => ()
    }
  }
}

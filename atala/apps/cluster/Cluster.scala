package atala.apps.cluster

import java.nio.file.Files

import atala.logging.Loggable
import atala.config._
import io.iohk.decco.Codec
import io.iohk.multicrypto._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.concurrent.Await
import scala.concurrent.duration._
import monix.execution.Scheduler.Implicits.global

case class Cluster[S, Tx: Codec: Loggable, Q: Loggable, QR: Loggable](n: Int, defaultState: S)(
    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]
) {

  private def address(host: String, port: Int): String =
    s"""{host: "$host", port: "$port"}"""

  private def remoteObftNode(serverIndex: Int, publicKey: SigningPublicKey): String =
    s"""|{
        | server-index: $serverIndex
        | public-key: "${publicKey.toCompactString}"
        | address: ${address("localhost", 8000 + serverIndex)}
        |}""".stripMargin

  private val keyPairs = (for (i <- 1 to n) yield (i, generateSigningKeyPair())).toMap
  private def obftNode(serverIndex: Int): String = {
    val allRemoteObftNodes =
      keyPairs
        .collect {
          case (i, ks) if i != serverIndex =>
            remoteObftNode(i, ks.public)
        }
        .mkString("[", ",\n", "]")
    val database = Files.createTempFile("iohk", serverIndex.toString).getFileName.toString

    s"""|server-index: $serverIndex
        |public-key: "${keyPairs(serverIndex).public.toCompactString}"
        |private-key: "${keyPairs(serverIndex).`private`.toCompactString}"
        |database: "$database"
        |remote-nodes: $allRemoteObftNodes
        |time-slot-duration: 300 millis
        |address: ${address("localhost", 8000 + serverIndex)}
        |""".stripMargin
  }

  def shutdown() = servers.map { _.shutdown() }

  private val servers: List[Server[S, Tx, Q, QR]] = {
    Await.result(
      scala.concurrent.Future.sequence(
        keyPairs.toList
          .map {
            case (i, ks) =>
              val configString = obftNode(i)
              val configObject = ConfigFactory.parseString(configString).resolve()
              val configuration = pureconfig.loadConfigOrThrow[ObftNode](configObject)
              val t =
                Server[S, Tx, Q, QR](configuration, defaultState)(processQuery, transactionExecutor)

              t.runAsync
          }
      ),
      10.seconds
    )

  }

  private def aServer(): Server[S, Tx, Q, QR] =
    servers(util.Random.nextInt(servers.length))

  def receiveTransaction(tx: Tx): Unit = {
    val ser = aServer()
    println(s"Server ${ser.configuration.serverIndex} received transaction $tx")
    tx sendTo ser
  }

  def ask(q: Q): (Int, QR) = {
    val server = aServer()
    (server.configuration.serverIndex, server.ask(q))
  }

  def askAll(q: Q): List[(Int, QR)] =
    servers.map(s => (s.configuration.serverIndex, s.ask(q)))

  def run(): Unit = {
    servers.foreach(_.run())
  }
}

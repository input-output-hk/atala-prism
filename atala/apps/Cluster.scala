package atala.apps

import java.nio.file.Files

import atala.logging.Loggable
import io.iohk.decco.Codec
import io.iohk.multicrypto._

import scala.concurrent.Future

case class Cluster[S, Tx: Codec: Loggable, Q, QR](n: Int, u: Int, defaultState: S)(
    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]
) {

  private val servers: List[Server[S, Tx, Q, QR]] =
    (1 to n).toList
      .map { i =>
        val database = Files.createTempFile("iohk", i.toString).getFileName.toString
        Server[S, Tx, Q, QR](i, generateSigningKeyPair(), n, n / 3, u, database, defaultState)(
          servers.map(_.publicKey),
          servers.filterNot(_.i == i).toSet
        )(processQuery, transactionExecutor)
      }

  private def aServer(): Server[S, Tx, Q, QR] =
    servers(util.Random.nextInt(servers.length))

  def receiveTransaction(tx: Tx): Unit =
    tx sendTo aServer

  def ask(q: Q): Future[QR] =
    aServer().ask(q)

  def run(): Unit =
    servers.foreach(_.run())
}

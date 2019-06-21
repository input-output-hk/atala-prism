package atala.apps

import java.nio.file.Files

import atala.logging.Loggable
import io.iohk.decco.Codec
import io.iohk.multicrypto._

import scala.concurrent.{ExecutionContext, Future}

case class Cluster[S, Tx: Codec: Loggable, Q: Loggable, QR: Loggable](n: Int, u: Int, defaultState: S)(
    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]
) {

  def shutdown() = servers.map { _.shutdown() }

  private val servers: List[Server[S, Tx, Q, QR]] = {
    val range = (1 to n).toList
    val keyPairs = for (x <- range) yield generateSigningKeyPair()
    range
      .map { i =>
        val database = Files.createTempFile("iohk", i.toString).getFileName.toString
        Server[S, Tx, Q, QR](i, keyPairs(i - 1), n, (n - 1) / 3, u, database, defaultState)(
          keyPairs.map(_.public),
          range.filterNot(_ == i).toSet
        )(processQuery, transactionExecutor)
      }
  }

  private def aServer(): Server[S, Tx, Q, QR] =
    servers(util.Random.nextInt(servers.length))

  def receiveTransaction(tx: Tx): Unit = {
    val ser = aServer()
    println(s"Server ${ser.i} received transaction $tx")
    tx sendTo ser
  }

  def ask(q: Q)(implicit ex: ExecutionContext): Future[(Int, QR)] = {
    val server = aServer()
    server.ask(q) map { (server.i, _) }
  }

  def askAll(q: Q)(implicit ex: ExecutionContext): Future[List[(Int, QR)]] =
    Future.traverse(servers)(s => s.ask(q).map { (s.i, _) })

  def run(): Unit = {
    servers.foreach(_.run())
  }
}

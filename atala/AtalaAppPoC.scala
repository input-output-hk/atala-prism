package atala

import java.nio.file.Files

import atala.obft._
import atala.ledger._
import atala.helpers.monixhelpers._
import io.iohk.decco.Codec
import io.iohk.multicrypto._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{MulticastStrategy, Observer, _}
import atala.clock._

import scala.concurrent.duration._
import scala.io.StdIn.readLine
import scala.concurrent.Future
import atala.logging._

case class Server[S, Tx: Codec: Loggable, Q, QR](
    i: Int,
    private val keyPair: SigningKeyPair,
    clusterSize: Int, // AKA 'n' in the paper
    maxNumOfAdversaries: Int, // AKA 't' in the paper
    transactionTTL: Int, // AKA 'u' in the paper
    database: String,
    defaultState: S
)(
    genesisKeys: => List[SigningPublicKey],
    otherServers: => Set[Server[S, Tx, Q, QR]]
)(
    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]
) {

  def publicKey: SigningPublicKey = keyPair.public

  def recieveTransaction(tx: Tx): Unit = {
    val m = NetworkMessage.AddTransaction[Tx](tx)
    inputNetwork.feedItem(m)

    // Replicate message to other servers
    difusingNetworkInput.feedItem(m)
  }

  def run(): Unit = {
    difusingNetworkStream.subscribe()
    ouroborosBFT.run()
    ledger.run()
  }

  private val inputNetwork: Observer[NetworkMessage[Tx]] with Observable[NetworkMessage[Tx]] =
    ConcurrentSubject[NetworkMessage[Tx]](MulticastStrategy.replay)

  private val (difusingNetworkInput, difusingNetworkStream) = {
    val input = ConcurrentSubject[NetworkMessage[Tx]](MulticastStrategy.replay)
    val stream =
      input
        .oneach { m =>
          otherServers.foreach { _.inputNetwork.feedItem(m) }
        }
    (input, stream)
  }

  private val stateRefreshInterval = 500.millis
  //private val delta = 5.seconds
  //private val delta = 1.seconds
  private val delta = 20.millis
  private val clockSignalsStream: Observable[Tick[Tx]] = {
    Observable
      .intervalWithFixedDelay(delta)
      .map { _ =>
        Tick(Clock.currentSlot(delta.toMillis))
      }
  }

  private lazy val ouroborosBFT =
    OuroborosBFT[Tx](
      i,
      keyPair,
      maxNumOfAdversaries,
      transactionTTL,
      genesisKeys,
      clockSignalsStream,
      inputNetwork,
      difusingNetworkInput,
      database
    )

  private lazy val ledger: Ledger[S, Tx, Q, QR] =
    Ledger(ouroborosBFT)(defaultState, stateRefreshInterval, delta)(processQuery, transactionExecutor)

  def ask(q: Q): Future[QR] = ledger.ask(q)
}

case class Cluster[S, Tx: Codec: Loggable, Q, QR](n: Int, u: Int, defaultState: S)(
    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]
) {

  import AtalaPoC.StringExtraOps

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

  def recieveTransaction(tx: Tx) =
    tx sendTo aServer

  def ask(q: Q): Future[QR] =
    aServer().ask(q)

  def run(): Unit =
    servers.foreach(_.run())
}

object AtalaPoC extends App {

  def transactionExecutor(accum: Map[Int, String], tx: (Int, String)): Option[Map[Int, String]] = {
    val (key, value) = tx
    if (accum.contains(key) && accum(key) != value) None
    else (Some(accum + tx))
  }

  def printCommand(): Unit = {
    val fr = cluster.ask(())
    val r = scala.concurrent.Await.result(fr, Duration.Inf)
    println()
    r.toList
      .sortBy(_._1)
      .foreach { case (k, v) => println(s"$k: $v") }
    println()
  }

  def ask[T](label: String, f: String => Option[T]): T = {
    while (true) {
      Console.print(s"$label > ")
      Console.flush
      readLine() match {
        case "exit" => sys.exit(1)
        case "print" => printCommand()
        case text =>
          f(text) match {
            case Some(t) => return t
            case None => println(s"\nNot a valid $label\n")
          }
      }
    }
    throw new Exception("Impossible")
  }

  import io.iohk.decco.auto._
  //val cluster = Cluster[Map[Int, String], (Int, String), Unit, Map[Int, String]](7, 5, Map.empty)((s, _) => s, transactionExecutor)
  val cluster =
    Cluster[Map[Int, String], (Int, String), Unit, Map[Int, String]](7, 14, Map.empty)((s, _) => s, transactionExecutor)

  cluster.run()

  println(
    """|
       |COMMANDS:
       |  At any point:
       |    - exit
       |      exits the application
       |    - print
       |      prints the view of the world that one of the servers has in its blockchain
       |
       |  When asked for 'index':
       |    Introduce an integer value and the next 'value' is going to be stored at that index in the blockchain
       |
       |  When asked for 'value':
       |    Introduce any string except 'print' or 'exit'. That string is going to be stored at the provided index in the blockchain
       |""".stripMargin
  )
  while (true) {
    val index = ask[Int]("index", s => util.Try(s.toInt).toOption)
    val message = ask[String]("value", Option.apply)
    (index, message) sendTo cluster
    Thread.sleep(10)
  }

  implicit class StringExtraOps[Tx](val tx: Tx) extends AnyVal {

    def sendTo[S, Q, QR](server: Server[S, Tx, Q, QR]): Unit =
      server.recieveTransaction(tx)

    def sendTo[S, Q, QR](cluster: Cluster[S, Tx, Q, QR]): Unit =
      cluster.recieveTransaction(tx)
  }

}

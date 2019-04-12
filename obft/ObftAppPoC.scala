package obft

import obft.clock._

import io.iohk.multicrypto._

import monix.execution.Scheduler.Implicits.global
import monix.reactive._
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{MulticastStrategy, Observer}

import scala.io.StdIn.readLine
import scala.concurrent.duration._
import io.iohk.decco.Codec

case class Server[Tx: Codec](
    i: Int,
    private val keyPair: SigningKeyPair,
    clusterSize: Int, // AKA 'n' in the paper
    maxNumOfAdversaries: Int, // AKA 't' in the paper
    transactionTTL: Int // AKA 'u' in the paper
)(
    genesisKeys: => List[SigningPublicKey],
    otherServers: => Set[Server[Tx]]
) {

  def publicKey: SigningPublicKey = keyPair.public

  def recieveTransaction(tx: Tx): Unit = {
    val m = Message.AddTransaction[Tx](tx)
    inputNetwork.feedItem(m)

    // Replicate message to other servers
    difusingNetworkInput.feedItem(m)
  }

  def run[S](initialState: S, transactionExecutor: (S, Tx) => Option[S]): S =
    ouroborosBFT.runAllFinalizedTransactions[S](Clock.currentSlot(delta), initialState, transactionExecutor)

  def start(): Unit = {
    difusingNetworkStream.subscribe()
    ouroborosBFT.ouroborosStream.subscribe()
  }

  private val inputNetwork: Observer[Message[Tx]] with Observable[Message[Tx]] =
    ConcurrentSubject[Message[Tx]](MulticastStrategy.replay)

  private val (difusingNetworkInput, difusingNetworkStream) = {
    val input = ConcurrentSubject[Message[Tx]](MulticastStrategy.replay)
    val stream =
      input
        .oneach { m =>
          otherServers.foreach { _.inputNetwork.feedItem(m) }
        }
    (input, stream)
  }

  //private val delta = 5.seconds
  //private val delta = 1.seconds
  private val delta = 20.millis
  private val clockSignalsStream: Observable[Tick] = {
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
      difusingNetworkInput
    )
}

case class Cluster[Tx: Codec](n: Int, u: Int) {

  import Obft.StringExtraOps

  private val servers: List[Server[Tx]] =
    (1 to n).toList
      .map(
        i =>
          Server[Tx](i, generateSigningKeyPair(), n, n / 3, u)(
            servers.map(_.publicKey),
            servers.filterNot(_.i == i).toSet
          )
      )

  private def aServer(): Server[Tx] =
    servers(util.Random.nextInt(servers.length))

  def recieveTransaction(tx: Tx) =
    tx sendTo aServer

  def run[S](initialState: S, transactionExecutor: (S, Tx) => Option[S]): S =
    aServer().run[S](initialState, transactionExecutor)

  def start(): Unit =
    servers.foreach(_.start())
}

object Obft extends App {

  def transactionExecutor(accum: Map[Int, String], tx: (Int, String)): Option[Map[Int, String]] = {
    val (key, value) = tx
    if (accum.contains(key) && accum(key) != value) None
    else (Some(accum + tx))
  }

  def printCommand(): Unit = {
    val r = cluster.run[Map[Int, String]](Map.empty, transactionExecutor)
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
  val cluster = Cluster[(Int, String)](7, 5)

  cluster.start()

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

    def sendTo(server: Server[Tx]): Unit =
      server.recieveTransaction(tx)

    def sendTo(cluster: Cluster[Tx]): Unit =
      cluster.recieveTransaction(tx)
  }

}

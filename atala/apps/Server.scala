package atala.apps

import atala.clock.Clock
import atala.view.StateView
import atala.logging.Loggable
import atala.obft.{OuroborosBFT, NetworkMessage, Tick}
import atala.network.{OBFTNetworkInterface, OBFTPeerGroupNetworkInterface}
import io.iohk.decco.Codec
import io.iohk.multicrypto._
import io.iohk.scalanet.peergroup.InetMultiAddress
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationLong

case class Server[S, Tx: Codec: Loggable, Q: Loggable, QR: Loggable](
    i: Int,
    private val keyPair: SigningKeyPair,
    clusterSize: Int, // AKA 'n' in the paper
    maxNumOfAdversaries: Int, // AKA 't' in the paper
    transactionTTL: Int, // AKA 'u' in the paper
    database: String,
    defaultState: S
)(
    genesisKeys: => List[SigningPublicKey],
    otherServers: => Set[Int]
)(
    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]
) {
  require(i > 0 && i <= clusterSize, "Invalid server id")

  def publicKey: SigningPublicKey = keyPair.public

  def receiveTransaction(tx: Tx): Unit = {
    feedMessage(NetworkMessage.AddTransaction[Tx](tx))
  }

  def feedMessage(m: NetworkMessage[Tx]): Unit = obftChannel.feed(m)

  def run(): Unit = {
    Await.result(networkInterface.initialise().runAsync, 10.seconds)
    ouroborosBFT.run()
    view.run()
  }

  private lazy val networkInterface =
    Server.NetworkInterface.createUDPInterface[Tx](i, otherServers)

  private lazy val obftChannel = Await.result(networkInterface.networkChannel().runAsync, 10.seconds)

  private val stateRefreshInterval = 500.millis
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
      Clock.currentSlot(delta.toMillis),
      keyPair,
      maxNumOfAdversaries,
      transactionTTL,
      genesisKeys,
      clockSignalsStream,
      obftChannel.in,
      obftChannel.out,
      database,
      delta.toMillis
    )

  private lazy val view: StateView[S, Tx, Q, QR] =
    StateView.inMemory(ouroborosBFT)(defaultState, stateRefreshInterval, delta)(processQuery, transactionExecutor)

  def ask(q: Q): Future[QR] = view.ask(q)

  def shutdown(): Unit = {
    obftChannel.close().runAsync
    networkInterface.shutdown().runAsync
  }
}

object Server {

  object NetworkInterface {
    def createUDPInterface[Tx: Codec](server: Int, rest: Set[Int]): OBFTNetworkInterface[InetMultiAddress, Tx] =
      OBFTPeerGroupNetworkInterface.createUPDNetworkInterface[Tx](server, rest)
  }

}

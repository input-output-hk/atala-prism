package atala.apps.kvnode

import atala.clock.Clock
import atala.logging.Loggable
import atala.obft.{OuroborosBFT, NetworkMessage, Tick}
import atala.network.{OBFTNetworkInterface, OBFTPeerGroupNetworkInterface}
import atala.config._
import io.iohk.decco.Codec
import io.iohk.multicrypto._
import io.iohk.scalanet.peergroup.InetMultiAddress
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import atala.helpers.monixhelpers._

import scala.concurrent.Await
import scala.concurrent.duration._

case class ObftController[S, Tx: Codec: Loggable, Q: Loggable, QR: Loggable](
    configuration: ObftNode,
    defaultState: S
)(
    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]
) {

  private def publicKey: SigningPublicKey = configuration.publicKey

  def receiveTransaction(tx: Tx): Unit = {
    feedMessage(NetworkMessage.AddTransaction[Tx](tx))
  }

  private def feedMessage(m: NetworkMessage[Tx]): Unit = obftChannel.feed(m)

  def start(): Unit = {
    Await.result(networkInterface.initialise().runAsync, 10.seconds)
    view
      .scan(defaultState) { (s, txSnap) =>
        transactionExecutor(s, txSnap.transaction).getOrElse(s)
      }
      .oneach { s =>
        state = s
      }
      .subscribe()
  }

  private lazy val networkInterface =
    ObftController.NetworkInterface.createUDPInterface[Tx](
      configuration.serverIndex,
      configuration.address,
      configuration.addressesOfRemoteNodes
    )

  private lazy val obftChannel = Await.result(networkInterface.networkChannel().runAsync, 10.seconds)
  private val clockSignalsStream: Observable[Tick[Tx]] = {
    Observable
      .intervalWithFixedDelay(configuration.timeSlotDuration)
      .map { _ =>
        Tick(Clock.currentSlot(configuration.timeSlotDuration.toMillis))
      }
  }

  private lazy val ouroborosBFT =
    OuroborosBFT[Tx](
      configuration.serverIndex,
      Clock.currentSlot(configuration.timeSlotDuration.toMillis),
      configuration.keyPair,
      configuration.genesisKeys,
      clockSignalsStream,
      obftChannel.in,
      obftChannel.out,
      configuration.database
    )

  private lazy val view = ouroborosBFT.view

  private var state: S = defaultState

  def ask(q: Q): QR = processQuery(state, q)

  def shutdown(): Unit = {
    obftChannel.close().runAsync
    networkInterface.shutdown().runAsync
  }
}

object ObftController {

  object NetworkInterface {
    def createUDPInterface[Tx: Codec](
        localNodeIndex: Int,
        localNodeAddress: ServerAddress,
        remoteNodes: Set[(Int, ServerAddress)]
    ): OBFTNetworkInterface[InetMultiAddress, Tx] =
      OBFTPeerGroupNetworkInterface.createUPDNetworkInterface[Tx](localNodeIndex, localNodeAddress, remoteNodes)
  }

}

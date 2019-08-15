package atala.apps.kvnode

import atala.clock.Clock
import atala.logging.Loggable
import atala.obft.{OuroborosBFT, NetworkMessage, Tick}
import atala.network._
import atala.config._
import io.iohk.decco.Codec
import io.iohk.multicrypto._
import io.iohk.scalanet.peergroup.InetMultiAddress
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import atala.helpers.monixhelpers._

import scala.concurrent.Await
import scala.concurrent.duration._
import monix.eval.Task

case class ObftController[S, Tx: Codec: Loggable, Q: Loggable, QR: Loggable] private (
    configuration: ObftNode,
    defaultState: S,
    networkInterface: OBFTNetworkInterface[Tx]
)(
    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]
) {

  private def publicKey: SigningPublicKey = configuration.publicKey

  def receiveTransaction(tx: Tx): Unit = {
    feedMessage(NetworkMessage.AddTransaction[Tx](tx))
  }

  private def feedMessage(m: NetworkMessage[Tx]): Unit = networkInterface.feed(m)

  def start(): Unit = {
    view
      .scan(defaultState) { (s, txSnap) =>
        transactionExecutor(s, txSnap.transaction).getOrElse(s)
      }
      .oneach { s =>
        state = s
      }
      .subscribe()
  }

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
      networkInterface.in,
      networkInterface.out,
      configuration.database
    )

  private lazy val view = ouroborosBFT.view

  private var state: S = defaultState

  def ask(q: Q): QR = processQuery(state, q)

  def shutdown(): Unit = {
    networkInterface.shutdown().runAsync
  }
}

object ObftController {

  def apply[S, Tx: Codec: Loggable, Q: Loggable, QR: Loggable](
      configuration: ObftNode,
      defaultState: S
  )(
      processQuery: (S, Q) => QR,
      transactionExecutor: (S, Tx) => Option[S]
  ): Task[ObftController[S, Tx, Q, QR]] = {
    val networkInterfaceFactory =
      OBFTNetworkFactory[Tx](
        configuration.serverIndex,
        configuration.address,
        configuration.addressesOfRemoteNodes
      )

    networkInterfaceFactory
      .initialise()
      .map { networkInterface =>
        new ObftController[S, Tx, Q, QR](
          configuration,
          defaultState,
          networkInterface
        )(processQuery, transactionExecutor)
      }
  }

}

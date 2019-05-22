package atala.apps

import atala.clock.Clock
import atala.helpers.monixhelpers._
import atala.ledger.Ledger
import atala.logging.Loggable
import atala.obft.{NetworkMessage, OuroborosBFT, Tick}
import io.iohk.decco.Codec
import io.iohk.multicrypto._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{MulticastStrategy, Observable, Observer}

import scala.concurrent.Future
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
    otherServers: => Set[Server[S, Tx, Q, QR]]
)(
    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]
) {
  require(i > 0 && i <= clusterSize, "Invalid server id")

  def publicKey: SigningPublicKey = keyPair.public

  def receiveTransaction(tx: Tx): Unit = {
    val m = NetworkMessage.AddTransaction[Tx](tx)
    inputNetwork.feedItem(m)

    // Replicate message to other servers
    diffusingNetworkInput.feedItem(m)
  }

  def run(): Unit = {
    diffusingNetworkStream.subscribe()
    ouroborosBFT.run()
    ledger.run()
  }

  private val inputNetwork: Observer[NetworkMessage[Tx]] with Observable[NetworkMessage[Tx]] =
    ConcurrentSubject[NetworkMessage[Tx]](MulticastStrategy.replay)

  private val (diffusingNetworkInput, diffusingNetworkStream) = {
    val input = ConcurrentSubject[NetworkMessage[Tx]](MulticastStrategy.replay)
    val stream =
      input
        .oneach { m =>
          otherServers.foreach { _.inputNetwork.feedItem(m) }
        }
    (input, stream)
  }

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
      keyPair,
      maxNumOfAdversaries,
      transactionTTL,
      genesisKeys,
      clockSignalsStream,
      inputNetwork,
      diffusingNetworkInput,
      database,
      delta.toMillis
    )

  private lazy val ledger: Ledger[S, Tx, Q, QR] =
    Ledger(ouroborosBFT)(defaultState, stateRefreshInterval, delta)(processQuery, transactionExecutor)

  def ask(q: Q): Future[QR] = ledger.ask(q)
}

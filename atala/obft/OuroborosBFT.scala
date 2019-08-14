package atala.obft

import atala.obft.common.TransactionSnapshot
import io.iohk.decco.Codec
import io.iohk.multicrypto._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive._
import atala.obft.blockchain._
import atala.clock._
import atala.obft.mempool._
import atala.helpers.monixhelpers._
import atala.logging._
import atala.obft.blockchain.models.ChainSegment

/** This class represents an instance of the Ouroboros BFT consensus protocol.
  *
  * @param blockchain an instance of the protocol core.
  * @param mempool an implementation of a mempool.
  * @param i the instance identifier (1 <= i <= clusterSize).
  * @param initialTimeSlot the initial time slot of the protocol.
  * @param keyPair the key pair associated to the current server.
  * @param clusterSize the number of servers involved in the protocol.
  * @param inputStreamClockSignals a stream of events that represent the progress of time.
  * @param inputStreamMessages a stream of network messages that come from other servers.
  * @param outputStreamDiffuseToRestOfCluster a stream of messages that will be diffused to the other servers.
  * @param codec$Tx an implicit Codec instance for Tx type.
  * @tparam Tx the type of the underlying transactions of the ledger constructed by the protocol.
  */
class OuroborosBFT[Tx: Codec](blockchain: Blockchain[Tx], mempool: MemPool[Tx])(
    i: Int,
    initialTimeSlot: TimeSlot,
    keyPair: SigningKeyPair,
    clusterSize: Int, // AKA 'n' in the paper
    inputStreamClockSignals: Observable[Tick[Tx]],
    inputStreamMessages: Observable[NetworkMessage[Tx]],
    outputStreamDiffuseToRestOfCluster: Observer[NetworkMessage.AddBlockchainSegment[Tx]],
    lastProcessedTimeSlot: TimeSlot
) extends AtalaLogging {

  private[obft] var currentTimeSlot: TimeSlot = initialTimeSlot
  private[obft] var eachTick: List[() => Unit] = Nil

  // Message processing methods
  // --------------------------

  // Main Ouroboros BFT Algorithm as specified in Figure 1 of the paper
  private def executeObftAlgorithm(event: ObftExternalActorMessage[Tx]): Unit = event match {

    case m: NetworkMessage.AddTransaction[Tx] => // 1. Mempool update
      logger.trace("AddTransaction message recieved in the Mempool")
      updateMempool(m)

    case m: NetworkMessage.AddBlockchainSegment[Tx] => // 2. Blockchain update
      logger.trace("AddBlockchainSegment message recieved in the Mempool")
      updateBlockchain(m)

    case tick: Tick[Tx] => // 3. Blockchain Extension
      logger.trace("OuroborosBFT notified of new tick", "tick" -> tick.timeSlot.toString)
      currentTimeSlot = tick.timeSlot
      extendBlockchain(tick)
      eachTick.foreach(action => action())

  }

  // Processes messages sent internally by OuroborosBFT itself
  private def executeInternalAction(action: ObftInternalActorMessage[Tx]): Unit = action match {

    case RequestStateUpdate(executable) => executable()

  }

  // The Streams
  // -----------

  private val actionsStream: Observer[ObftInternalActorMessage[Tx]] with Observable[ObftInternalActorMessage[Tx]] =
    ConcurrentSubject[ObftInternalActorMessage[Tx]](MulticastStrategy.replayLimited(512))

  // OuroborosBFT is implemented around the concept of an 'actor' implemented with a
  // monix stream. This is that stream
  private[obft] val obftActorStream =
    Observable
      .apply(
        inputStreamClockSignals,
        inputStreamMessages,
        actionsStream
      )
      .merge
      .oneach {
        case message: ObftExternalActorMessage[Tx] =>
          executeObftAlgorithm(message)
        case action: ObftInternalActorMessage[Tx] =>
          executeInternalAction(action)
      }

  def run(): Unit =
    obftActorStream.subscribe()

  def view(lastseenTimestamp: TimeSlot): Observable[TransactionSnapshot[Tx]] = {
    val r = ViewController(lastseenTimestamp)

    val onTick: () => Unit = { () =>
      r.tick()
    }

    actionsStream.feedItem(RequestStateUpdate(() => eachTick = onTick :: eachTick))

    r.view
  }

  def view: Observable[TransactionSnapshot[Tx]] = {
    view(TimeSlot.zero)
  }

  // Helper methods
  // -------------

  private type Transactions = Observer[TransactionSnapshot[Tx]] with Observable[TransactionSnapshot[Tx]]

  private case class ViewController(lastseenTimestamp: TimeSlot) {
    private val r: Transactions =
      ConcurrentSubject[TransactionSnapshot[Tx]](MulticastStrategy.replayLimited(512))

    def view: Observable[TransactionSnapshot[Tx]] = r

    private var lastTimestamp = lastseenTimestamp

    def tick(): Unit = {
      lastTimestamp = blockchain.foreachFromLastseen(currentTimeSlot, lastTimestamp, snapshot => r.feedItem(snapshot))
    }
  }

  private def IamLeader(at: TimeSlot): Boolean =
    at.leader(clusterSize) == i

  private def updateMempool(message: NetworkMessage.AddTransaction[Tx]): Unit =
    mempool.add(message.tx)

  private def updateBlockchain(message: NetworkMessage.AddBlockchainSegment[Tx]): Unit =
    blockchain.add(message.chainSegment, currentTimeSlot)

  private def extendBlockchain(tick: Tick[Tx]): Unit = {
    if (IamLeader(tick.timeSlot)) {
      logger.debug("I become leader", "tick" -> tick.timeSlot.toString)
      val transactions = mempool.collect()
      if (transactions.nonEmpty) {
        logger.debug(
          "There are transactions in the MemPool. Ready to generate a block",
          "tick" -> tick.timeSlot.toString
        )
        val blockData = blockchain.createBlockData(transactions, tick.timeSlot, keyPair.`private`)
        val segment = ChainSegment(blockData)

        blockchain.add(segment, tick.timeSlot)

        outputStreamDiffuseToRestOfCluster
          .feedItem(NetworkMessage.AddBlockchainSegment(segment))
      }

    }

    mempool.advance()
  }

}

object OuroborosBFT {

  /**

      Generates an instance of OuroborosBFT with all its external dependencies properly injected. To
      generate an instance of OuroborosBFT and specify the external dependencies by hand (for example for
      testing purposes) use the constructor.

    */
  def apply[Tx: Codec: Loggable](
      i: Int,
      initialTimeSlot: TimeSlot,
      keyPair: SigningKeyPair,
      genesisKeys: List[SigningPublicKey],
      inputStreamClockSignals: Observable[Tick[Tx]],
      inputStreamMessages: Observable[NetworkMessage[Tx]],
      outputStreamDiffuseToRestOfCluster: Observer[NetworkMessage.AddBlockchainSegment[Tx]],
      database: String,
      lastProcessedTimeSlot: TimeSlot = TimeSlot.zero
  ): OuroborosBFT[Tx] = {

    // AKA 'n' in the paper
    val clusterSize = genesisKeys.length

    // AKA 't' in the paper
    val maxNumOfAdversaries: Int = (clusterSize - 1) / 3; // if `n = 3t + 1` then t must be `(n - 1) / 3`

    // AKA 'u' in the paper
    val transactionTTL: Int = (maxNumOfAdversaries * 5) + 2; // As expressed in the Theorem 3.8 of the paper

    val blockchain = Blockchain[Tx](genesisKeys, maxNumOfAdversaries, database, SegmentValidator(genesisKeys))
    val mempool: MemPool[Tx] = MemPool[Tx](transactionTTL)

    new OuroborosBFT[Tx](blockchain, mempool)(
      i,
      initialTimeSlot,
      keyPair,
      clusterSize,
      inputStreamClockSignals,
      inputStreamMessages,
      outputStreamDiffuseToRestOfCluster,
      lastProcessedTimeSlot
    )

  }
}

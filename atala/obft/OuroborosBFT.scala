package atala.obft

// format: off

import atala.obft.common._
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
import atala.state.{StateGate => FullStateGate}

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
  outputStreamDiffuseToRestOfCluster: Observer[NetworkMessage.AddBlockchainSegment[Tx]]
) extends AtalaLogging {

  private[obft] var currentTimeSlot: TimeSlot = initialTimeSlot

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

  }

  // Processes messages sent internally by OuroborosBFT itself
  private def executeInternalAction(action: ObftInternalActorMessage[Tx]): Unit = action match {

    case RequestStateUpdate(executable) => executable()

  }



  // The Streams
  // -----------

  private val actionsStream: Observer[ObftInternalActorMessage[Tx]] with Observable[ObftInternalActorMessage[Tx]] =
    ConcurrentSubject[ObftInternalActorMessage[Tx]](MulticastStrategy.replay)

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



  // Helper methods
  // -------------

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
        logger.debug("There are transactions in the MemPool. Ready to generate a block", "tick" -> tick.timeSlot.toString)
        val blockData = blockchain.createBlockData(transactions, tick.timeSlot, keyPair.`private`)
        val segment = ChainSegment(blockData)

        blockchain.add(segment, tick.timeSlot)

        outputStreamDiffuseToRestOfCluster
          .feedItem(NetworkMessage.AddBlockchainSegment(segment))
      }

    }

    mempool.advance()
  }




  //
  // STATE GATE
  //  A gate is the way to access the information stored in the transaction log hold by the consensus algorithm
  //
  private def gate[S] (
    actionsStream: Observer[ObftInternalActorMessage[Tx]] with Observable[ObftInternalActorMessage[Tx]],
    blockchain: Blockchain[Tx] )(
    prepareExecution: ( /*now:*/ TimeSlot, /*previousSnapshot:*/ StateSnapshot[S]) => S,
    transactionExecutor: (S, Tx) => Option[S], // transactionExecutor refers to the `parser` concept in the paper
    finalizeExecution: StateSnapshot[S] => Unit,
    useFinalizedTransactions: Boolean = true ): StateGate[S] = {

    val newActionsStream: Observer[FullStateGate.Callback] = actionsStream.contramap(RequestStateUpdate[Tx](_))
    val prepareComputation: FullStateGate.StateComputationStage[S, TimeSlot] = prepareExecution
    val computeUpdatedState: FullStateGate.StateComputationStage[S, TimeSlot] = (now, snapshot) => {
      if(useFinalizedTransactions)
        blockchain.runFinalizedTransactionsFromPreviousStateSnapshot(now, snapshot, transactionExecutor)
      else
        blockchain.unsafeRunTransactionsFromPreviousStateSnapshot(snapshot, transactionExecutor)
    }
    val finalizeComputation: FullStateGate.StateComputationStage[S, TimeSlot] = (_, snap) => {finalizeExecution(snap); snap.computedState}

    new FullStateGate[S, TimeSlot](newActionsStream)(prepareComputation, computeUpdatedState, finalizeComputation)
  }

  def gate[S](
    prepareExecution: (/*now:*/ TimeSlot, /*previousSnapshot:*/ StateSnapshot[S]) => S,
    transactionExecutor: (S, Tx) => Option[S],
    finalizeExecution: /*result:*/ StateSnapshot[S] => Unit,
    useFinalizedTransactions: Boolean): StateGate[S] =
    gate[S](actionsStream, blockchain)(prepareExecution, transactionExecutor, finalizeExecution, useFinalizedTransactions)

  def gate[S](
    prepareExecution: (/*now:*/ TimeSlot, /*previousSnapshot:*/ StateSnapshot[S]) => S,
    transactionExecutor: (S, Tx) => Option[S],
    finalizeExecution: /*result:*/ StateSnapshot[S] => Unit): StateGate[S] =
    gate(prepareExecution, transactionExecutor, finalizeExecution, true)

  def gate[S](transactionExecutor: (S, Tx) => Option[S], useFinalizedTransactions: Boolean): StateGate[S] = {
    def initialState(t: TimeSlot, s: StateSnapshot[S]): S = s.computedState
    def doNothing(s: StateSnapshot[S]): Unit = ()
    gate(initialState _, transactionExecutor, doNothing _, useFinalizedTransactions)
  }

  def gate[S](transactionExecutor: (S, Tx) => Option[S]): StateGate[S] =
    gate(transactionExecutor, true)
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
      maxNumOfAdversaries: Int, // AKA 't' in the paper
      transactionTTL: Int, // AKA 'u' in the paper
      genesisKeys: List[SigningPublicKey],
      inputStreamClockSignals: Observable[Tick[Tx]],
      inputStreamMessages: Observable[NetworkMessage[Tx]],
      outputStreamDiffuseToRestOfCluster: Observer[NetworkMessage.AddBlockchainSegment[Tx]],
      database: String,
      slotDuration: Long
  ): OuroborosBFT[Tx] = {

    val blockchain = Blockchain[Tx](genesisKeys, maxNumOfAdversaries, database, SegmentValidator(genesisKeys))
    val mempool: MemPool[Tx] = MemPool[Tx](transactionTTL)
    val clusterSize = genesisKeys.length // AKA 'n' in the paper

    new OuroborosBFT[Tx](blockchain, mempool)(
      i,
      initialTimeSlot,
      keyPair,
      clusterSize,
      inputStreamClockSignals,
      inputStreamMessages,
      outputStreamDiffuseToRestOfCluster
    )
  }
}

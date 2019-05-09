package atala.obft

// format: off

import io.iohk.decco.Codec
import io.iohk.multicrypto._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive._
import atala.obft.blockchain._
import atala.clock._
import atala.obft.mempool._
import atala.helpers.monixhelpers._

class OuroborosBFT[Tx: Codec](blockchain: Blockchain[Tx], mempool: MemPool[Tx])(
    i: Int,
    keyPair: SigningKeyPair,
    clusterSize: Int, // AKA 'n' in the paper
    inputStreamClockSignals: Observable[Tick[Tx]],
    inputStreamMessages: Observable[NetworkMessage[Tx]],
    outputStreamDiffuseToRestOfCluster: Observer[NetworkMessage.AddBlockchainSegment[Tx]]
) {



  // Message processing methods
  // --------------------------

  // Main Ouroboros BFT Algorithm as specified in Figure 1 of the paper
  private def executeObftAlgorithm(event: ObftExternalActorMessage[Tx]): Unit = event match {

    case m: NetworkMessage.AddTransaction[Tx] => // 1. Mempool update
      updateMempool(m)

    case m: NetworkMessage.AddBlockchainSegment[Tx] => // 2. Blockchain update
      updateBlockchain(m)

    case tick: Tick[Tx] => // 3. Blockchain Extension
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
    blockchain.add(message.chainSegment)

  private def extendBlockchain(tick: Tick[Tx]): Unit = {
    if (IamLeader(tick.timeSlot)) {
      val transactions = mempool.collect()
      if (transactions.nonEmpty) {
        val blockData = blockchain.createBlockData(transactions, tick.timeSlot, keyPair.`private`)
        val segment = List(blockData)

        blockchain.add(segment)

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

  def StateGate[S](transactionExecutor: (S, Tx) => Option[S], useFinalizedTransactions: Boolean = true): StateGate[S, Tx] =
    new StateGate[S, Tx](actionsStream, blockchain)(transactionExecutor, useFinalizedTransactions)
}




object OuroborosBFT {


  /**

      Generates an instance of OuroborosBFT with all its external dependencies properly injected. To
      generate an instance of OuroborosBFT and specify the external dependencies by hand (for example for
      testing purposes) use the constructor.

    */
  def apply[Tx: Codec](
      i: Int,
      keyPair: SigningKeyPair,
      maxNumOfAdversaries: Int, // AKA 't' in the paper
      transactionTTL: Int, // AKA 'u' in the paper
      genesisKeys: List[SigningPublicKey],
      inputStreamClockSignals: Observable[Tick[Tx]],
      inputStreamMessages: Observable[NetworkMessage[Tx]],
      outputStreamDiffuseToRestOfCluster: Observer[NetworkMessage.AddBlockchainSegment[Tx]],
      database: String
  ): OuroborosBFT[Tx] = {

    val blockchain = Blockchain[Tx](genesisKeys, maxNumOfAdversaries, database)
    val mempool: MemPool[Tx] = MemPool[Tx](transactionTTL)
    val clusterSize = genesisKeys.length // AKA 'n' in the paper

    new OuroborosBFT[Tx](blockchain, mempool)(
      i,
      keyPair,
      clusterSize,
      inputStreamClockSignals,
      inputStreamMessages,
      outputStreamDiffuseToRestOfCluster
    )
  }
}

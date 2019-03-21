package obft

import obft.blockchain._

import monix.reactive._
import monix.execution.Scheduler.Implicits.global

final case class Tick(timeSlot: TimeSlot)

sealed trait Message[Tx]
object Message {

  case class AddTransaction[Tx](tx: Tx) extends Message[Tx]
  case class AddBlockchainSegment[Tx](chainSegment: List[Block[Tx]]) extends Message[Tx]

}

class OuroborosBFT[Tx](blockchain: Blockchain[Tx], mempool: MemPool[Tx])(
    i: Int,
    keyPair: KeyPair,
    clusterSize: Int, // AKA 'n' in the paper
    inputStreamClockSignals: Observable[Tick],
    inputStreamMessages: Observable[Message[Tx]],
    outputStreamDiffuseToRestOfCluster: Observer[Message.AddBlockchainSegment[Tx]]
) {

  private def IamLeader(at: TimeSlot): Boolean =
    at.leader(clusterSize) == i

  private def updateMempool(message: Message.AddTransaction[Tx]): Unit =
    mempool.add(message.tx)

  private def updateBlockchain(message: Message.AddBlockchainSegment[Tx]): Unit =
    blockchain.add(message.chainSegment)

  private def extendBlockchain(tick: Tick): Unit = {
    if (IamLeader(tick.timeSlot)) {
      val transactions = mempool.collect()
      if (transactions.nonEmpty) {
        val blockData = blockchain.createBlockData(transactions, tick.timeSlot, keyPair.PrivateKey)
        val segment = List(blockData)

        blockchain.add(segment)

        outputStreamDiffuseToRestOfCluster
          .feedItem(Message.AddBlockchainSegment(segment))
      }

    }

    mempool.advance()
  }

  def unsafeRunAllTransactions[S](initialState: S, transactionExecutor: (S, Tx) => Option[S]): S =
    blockchain.unsafeRunAllTransactions[S](initialState, transactionExecutor)

  def runAllFinalizedTransactions[S](now: TimeSlot, initialState: S, transactionExecutor: (S, Tx) => Option[S]): S =
    blockchain.runAllFinalizedTransactions[S](now: TimeSlot, initialState, transactionExecutor)

  val ouroborosStream = {
    Observable
      .apply(
        inputStreamClockSignals.map(Right.apply),
        inputStreamMessages.map(Left.apply)
      )
      .merge
      .oneach { // Main Ouroboros BFT Algorithm as specified in Figure 1 of the paper

        case Left(m: Message.AddTransaction[Tx]) => // 1. Mempool update
          updateMempool(m)

        case Left(m: Message.AddBlockchainSegment[Tx]) => // 2. Blockchain update
          updateBlockchain(m)

        case Right(tick) => // 3. Blockchain Extension
          extendBlockchain(tick)
      }
  }
}

object OuroborosBFT {
  def apply[Tx](
      i: Int,
      keyPair: KeyPair,
      maxNumOfAdversaries: Int, // AKA 't' in the paper
      transactionTTL: Int, // AKA 'u' in the paper
      genesisKeys: List[PublicKey],
      inputStreamClockSignals: Observable[Tick],
      inputStreamMessages: Observable[Message[Tx]],
      outputStreamDiffuseToRestOfCluster: Observer[Message.AddBlockchainSegment[Tx]]
  ): OuroborosBFT[Tx] = {
    val blockchain = Blockchain[Tx](genesisKeys, maxNumOfAdversaries)
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

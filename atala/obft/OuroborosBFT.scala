package atala.obft

import java.nio.file.Path

import io.iohk.decco.Codec
import io.iohk.multicrypto._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive._
import atala.obft.blockchain._
import atala.clock._
import atala.obft.mempool._
import atala.helpers.monixhelpers._
import atala.obft.blockchain.models._

final case class Tick(timeSlot: TimeSlot)

sealed trait Message[Tx]
object Message {

  case class AddTransaction[Tx](tx: Tx) extends Message[Tx]
  case class AddBlockchainSegment[Tx](chainSegment: List[Block[Tx]]) extends Message[Tx]

}

class OuroborosBFT[Tx: Codec](blockchain: Blockchain[Tx], mempool: MemPool[Tx])(
    i: Int,
    keyPair: SigningKeyPair,
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
        val blockData = blockchain.createBlockData(transactions, tick.timeSlot, keyPair.`private`)
        val segment = List(blockData)

        blockchain.add(segment)

        outputStreamDiffuseToRestOfCluster
          .feedItem(Message.AddBlockchainSegment(segment))
      }

    }

    mempool.advance()
  }

  def unsafeRunAllTransactions[S](initialState: S, transactionExecutor: (S, Tx) => Option[S]): S =
    blockchain.unsafeRunAllTransactions(initialState, transactionExecutor)

  def unsafeRunTransactionsFromPreviousStateSnapshot[S](
      snapshot: StateSnapshot[S],
      transactionExecutor: (S, Tx) => Option[S]
  ): S =
    blockchain.unsafeRunTransactionsFromPreviousStateSnapshot(snapshot, transactionExecutor)

  def runAllFinalizedTransactions[S](now: TimeSlot, initialState: S, transactionExecutor: (S, Tx) => Option[S]): S =
    blockchain.runAllFinalizedTransactions(now, initialState, transactionExecutor)

  def runFinalizedTransactionsFromPreviousStateSnapshot[S](
      now: TimeSlot,
      snapshot: StateSnapshot[S],
      transactionExecutor: (S, Tx) => Option[S]
  ): S =
    blockchain.runFinalizedTransactionsFromPreviousStateSnapshot(now, snapshot, transactionExecutor)

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
  def apply[Tx: Codec](
      i: Int,
      keyPair: SigningKeyPair,
      maxNumOfAdversaries: Int, // AKA 't' in the paper
      transactionTTL: Int, // AKA 'u' in the paper
      genesisKeys: List[SigningPublicKey],
      inputStreamClockSignals: Observable[Tick],
      inputStreamMessages: Observable[Message[Tx]],
      outputStreamDiffuseToRestOfCluster: Observer[Message.AddBlockchainSegment[Tx]],
      storageFile: Path
  ): OuroborosBFT[Tx] = {
    val blockchain = Blockchain[Tx](genesisKeys, maxNumOfAdversaries, storageFile)
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

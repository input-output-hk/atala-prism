package obft.blockchain

// format: off

import io.iohk.multicrypto._
import io.iohk.decco.auto._
import io.iohk.decco._

import obft.blockchain.storage.{BlockStorage, InMemoryBlockStorage}
import obft.clock._

import scala.annotation.tailrec

class Blockchain[Tx: Codec](validator: SegmentValidator, private[blockchain] val storage: BlockStorage[Tx])(keys: List[SigningPublicKey], maxNumOfAdversaries: Int) { blockchain =>

  private[blockchain] case class BlockPointer(
    at: Hash,
    blockchainLength: Int // I wonder if this should be a Long
  ) {
    def forceGetPointedBlockFromStorage: AnyBlock[Tx] =
      forceGetFromStorage(at)
  }

  private[blockchain] var headPointer: BlockPointer = {
    val gb: AnyBlock[Tx] = GenesisBlock[Tx](keys)
    val hgb = hash(gb)
    storage.put(hgb, gb)
    BlockPointer(hgb, 0)
  }

  private def head: AnyBlock[Tx] = headPointer.forceGetPointedBlockFromStorage

  private val finalisedDelta = (3 * maxNumOfAdversaries) + 1
  private def finalizedHeadPointer(now: TimeSlot): BlockPointer = {
    val firstInvalidTimeSlot = now - finalisedDelta
    def findPointer(p: BlockPointer): BlockPointer =
      p.forceGetPointedBlockFromStorage match {
        case GenesisBlock(_) => p
        case Block(body, _) if body.timeSlot < firstInvalidTimeSlot => p
        case Block(body, _) =>
          findPointer(BlockPointer(body.hash, p.blockchainLength - 1))
      }

    findPointer(headPointer)
  }
  private def finalizedHead(now: TimeSlot): AnyBlock[Tx] = finalizedHeadPointer(now).forceGetPointedBlockFromStorage

  def unsafeRunAllTransactions[S](initialState: S, transactionExecutor: (S, Tx) => Option[S]): S = {
    run(headPointer, StateSnapshot(initialState, TimeSlot.zero), transactionExecutor)
  }

  def unsafeRunTransactionsFromPreviousStateSnapshot[S](snapshot: StateSnapshot[S], transactionExecutor: (S, Tx) => Option[S]): S = {
    run(headPointer, snapshot, transactionExecutor)
  }

  def runAllFinalizedTransactions[S](now: TimeSlot, initialState: S, transactionExecutor: (S, Tx) => Option[S]): S = {
    run(finalizedHeadPointer(now), StateSnapshot(initialState, TimeSlot.zero), transactionExecutor)
  }

  def runFinalizedTransactionsFromPreviousStateSnapshot[S](now: TimeSlot, snapshot: StateSnapshot[S], transactionExecutor: (S, Tx) => Option[S]): S = {
    run(finalizedHeadPointer(now), snapshot, transactionExecutor)
  }

  private[blockchain] def run[S](lastBlockPointer: BlockPointer, snapshot: StateSnapshot[S], transactionExecutor: (S, Tx) => Option[S]): S = {
    val initialState = snapshot.computedState
    val snapshotTimestamp = snapshot.snapshotTimestamp
    def safeTransactionExecutor(state: S, tx: Tx): S =
      transactionExecutor(state, tx).getOrElse(state)

    @tailrec
    def run(pointer: AnyBlock[Tx], accum: List[Tx]): S =
      pointer match {
        case GenesisBlock(_) =>
          accum.foldLeft(initialState)(safeTransactionExecutor)
        case Block(body, _) if body.timeSlot <= snapshotTimestamp =>
          accum.foldLeft(initialState)(safeTransactionExecutor)
        case Block(body, _) =>
          val newAccum = body.delta ++ accum
          run(forceGetFromStorage(body.hash), newAccum)
      }

    run(lastBlockPointer.forceGetPointedBlockFromStorage, Nil)
  }

  def add(chainSegment: List[Block[Tx]]): Unit = {

    val reversedSegment = chainSegment.reverse

    @tailrec
    def findBlocksToRemove(from: Hash, previous: Hash, accum: List[Hash]): List[Hash] =
      if (from == previous)
        accum
      else
        forceGetFromStorage(from) match {
          case GenesisBlock(_) =>
            throw new Error("FATAL: It's impossible it exist a block that goes before the GenesisBlock")
          case Block(body, _) =>
            findBlocksToRemove(body.hash, previous, from :: accum)
        }

    reversedSegment match {
      case Nil => ()
      case h :: _ =>
        storage.get(h.body.hash) match {
          case None =>
            // The segment follows up from a block we don't have in the storage
            ()
          case Some(previous) =>
            if (!validator.isValid(chainSegment, h.body.hash)) return ()
            val blocksToRemove = findBlocksToRemove(headPointer.at, h.body.hash, Nil)

            val s0 = headPointer.blockchainLength - blocksToRemove.length
            val s  = s0 + chainSegment.length
            val l  = headPointer.blockchainLength

            if (s > l) {  // THE PAPER: Whenever the server becomes aware of an alternative blockchain
                          //            B0 B'1 ... B's with s > l, it replaces its local chain with this
                          //            new chain provided it is valid

              chainSegment.foreach(b => storage.put(hash(b), b))
              blocksToRemove.foreach(storage.remove)
              headPointer = BlockPointer(hash(chainSegment.head), s)
            }
        }
    }


  }

  def createBlockData(transactions: List[Tx], timeSlot: TimeSlot, key: SigningPrivateKey): Block[Tx] = {
    val body =
      BlockBody[Tx](
        hash(head),
        transactions,
        timeSlot,
        sign(timeSlot, key)
      )

    Block[Tx](
      body,
      sign(body, key)
    )
  }

  private def forceGetFromStorage(id: Hash): AnyBlock[Tx] =
    storage.get(id) match {
      case None =>
        throw new Error("FATAL: A block that in theory was already stored in the blockchain could not be recovered")
      case Some(block) =>
        block
    }

}

object Blockchain {

  def apply[Tx: Codec](keys: List[SigningPublicKey], maxNumOfAdversaries: Int): Blockchain[Tx] =
    new Blockchain[Tx](new SegmentValidator(keys), new InMemoryBlockStorage[Tx]())(keys, maxNumOfAdversaries)

}

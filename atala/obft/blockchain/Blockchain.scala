package atala.obft.blockchain

// format: off

import atala.clock._
import atala.obft.blockchain.models._
import atala.obft.blockchain.storage._
import io.iohk.decco._
import io.iohk.decco.auto._
import io.iohk.multicrypto._

import scala.annotation.tailrec

class Blockchain[Tx: Codec](validator: SegmentValidator, private[blockchain] val storage: BlockStorage[Tx])(keys: List[SigningPublicKey], maxNumOfAdversaries: Int) { blockchain =>

  private[blockchain] case class BlockPointer(
    at: Hash,
    blockchainHeight: Height
  ) {
    def forceGetPointedBlockFromStorage: AnyBlock[Tx] =
      forceGetFromStorage(at)
  }

  private[blockchain] def blockchainHeight(): Height = {
    val numberOfBlocks: Int = storage.getNumberOfBlocks()
    Height.from(numberOfBlocks) getOrElse { throw new RuntimeException("getNumberOfBlocks: returned an invalid number of blocks") }
  }

  private[blockchain] def getLatestBlockFromStorage(): AnyBlock[Tx] =
    storage.getLatestBlock() getOrElse genesisBlock

  val genesisBlock = GenesisBlock[Tx](keys)
  val genesisBlockHash = hash(genesisBlock)

  private[blockchain] var headPointer: BlockPointer = {
    BlockPointer(hash(getLatestBlockFromStorage()), blockchainHeight())
  }

  private def head: AnyBlock[Tx] = headPointer.forceGetPointedBlockFromStorage

  private val finalisedDelta = (3 * maxNumOfAdversaries) + 1
  private def finalizedHeadPointer(now: TimeSlot): BlockPointer = {
    val firstInvalidTimeSlot = (now - finalisedDelta) getOrElse TimeSlot.zero
    @scala.annotation.tailrec
    def findPointer(p: BlockPointer): BlockPointer =
      p.forceGetPointedBlockFromStorage match {
        case GenesisBlock(_) => p
        case Block(body, _) if body.timeSlot < firstInvalidTimeSlot => p
        case Block(body, _) =>
          val height: Height =
            p.blockchainHeight.below getOrElse { throw new RuntimeException("Fatal: Non genesis block with height zero")}
          findPointer(BlockPointer(body.previousHash, height))
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
          run(forceGetFromStorage(body.previousHash), newAccum)
      }

    run(lastBlockPointer.forceGetPointedBlockFromStorage, Nil)
  }

  def add(chainSegment: ChainSegment[Tx]): Unit = {

    @tailrec
    def listBlocksToRemove(from: Hash, limit: Hash, accum: List[Hash]): List[Hash] =
      if (from == limit)
        accum
      else
        forceGetFromStorage(from) match {
          case GenesisBlock(_) =>
            throw new Error("FATAL: It's impossible it exist a block that goes before the GenesisBlock")
          case Block(body, _) =>
            listBlocksToRemove(body.previousHash, limit, from :: accum)
        }

    // Note:
    //   if getFromStorage(oldestBlock.body.previousHash) is None, then the chainSegment
    //   starts at an unknown point in the chain. We are currently ignoring this fact, but
    //   this could indicate that we are missing valid blocks.
    for {
      oldestBlock <- chainSegment.oldestBlock
      blockPreviousToTheSegment <- getFromStorage(oldestBlock.body.previousHash)
      if validator.isValid(chainSegment, blockPreviousToTheSegment)
    } {
      val lastAcceptedBlock = headPointer.at
      val limit = oldestBlock.body.previousHash
      val blocksToRemove = listBlocksToRemove(lastAcceptedBlock, limit, Nil)

      val s0 = headPointer.blockchainHeight.toInt - blocksToRemove.length
      val s = s0 + chainSegment.length
      val l = headPointer.blockchainHeight.toInt

      if (s > l) { // THE PAPER: Whenever the server becomes aware of an alternative blockchain
        //            B0 B'1 ... B's with s > l, it replaces its local chain with this
        //            new chain provided it is valid

        val blocksToAdd = chainSegment.blocks map { b => (hash(b), b) }
        storage.update(blocksToRemove, blocksToAdd)

        val finalHeight = Height.from(s).get // `.get` is safe as s > 0.
        headPointer = BlockPointer(hash(chainSegment.mostRecentBlock.get), finalHeight)
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

  private def forceGetFromStorage(id: Hash): AnyBlock[Tx] = {
    getFromStorage(id)
        .getOrElse { throw new Error("FATAL: A block that in theory was already stored in the blockchain could not be recovered") }
  }

  private def getFromStorage(id: Hash): Option[AnyBlock[Tx]] = {
    if (genesisBlockHash == id) {
      Some(genesisBlock)
    } else {
      storage.get(id)
    }
  }

  def height: Height = headPointer.blockchainHeight
}

object Blockchain {

  def apply[Tx: Codec](keys: List[SigningPublicKey], maxNumOfAdversaries: Int, database: String, segmentValidator: SegmentValidator): Blockchain[Tx] = {
    val storage = H2BlockStorage[Tx](database)
    new Blockchain[Tx](segmentValidator, storage)(keys, maxNumOfAdversaries)
  }
}

package atala.obft.blockchain

// format: off

import atala.obft.common._
import atala.clock._
import atala.logging.{AtalaLogging, Loggable}
import atala.obft.blockchain.models._
import atala.obft.blockchain.storage._
import io.iohk.decco._
import io.iohk.decco.auto._
import io.iohk.multicrypto._

import scala.annotation.tailrec

class Blockchain[Tx: Codec: Loggable](validator: SegmentValidator, private[blockchain] val storage: BlockStorage[Tx])
                           (keys: List[SigningPublicKey], maxNumOfAdversaries: Int) extends AtalaLogging { blockchain =>

  import Blockchain.LoggingFormat._

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
    logger.info("Retrieving head pointer from db")
    val latestBlock = getLatestBlockFromStorage()
    val latestBlockHash = hash(latestBlock)
    val chainHeight = blockchainHeight()
    logger.info("Head pointer retrieved successfully", "blockHash" -> latestBlockHash, "chainHeight" -> chainHeight)
    BlockPointer(latestBlockHash, chainHeight)
  }

  private def head: AnyBlock[Tx] = headPointer.forceGetPointedBlockFromStorage

  private val finalisedDelta = (3 * maxNumOfAdversaries) + 1
  private def finalizedHeadPointer(now: TimeSlot): BlockPointer = {
    logger.trace("Calculating block pointer for finalized transactions", "current time slot" -> now)
    val firstInvalidTimeSlot = (now - finalisedDelta) getOrElse TimeSlot.zero
    @scala.annotation.tailrec
    def findPointer(p: BlockPointer): BlockPointer =
      p.forceGetPointedBlockFromStorage match {
        case GenesisBlock(_) =>
          logger.debug(s"Block pointer computed", "block pointer hash" -> p.at, "pointer height" -> p.blockchainHeight)
          p
        case Block(body, _) if body.timeSlot < firstInvalidTimeSlot =>
          logger.debug(s"Block pointer computed", "block pointer hash" -> p.at, "pointer height" -> p.blockchainHeight)
          p
        case Block(body, _) =>
          val height: Height =
            p.blockchainHeight.below getOrElse { throw new RuntimeException("Fatal: Non genesis block with height zero")}
          findPointer(BlockPointer(body.previousHash, height))
      }

    findPointer(headPointer)
  }
  private def finalizedHead(now: TimeSlot): AnyBlock[Tx] = finalizedHeadPointer(now).forceGetPointedBlockFromStorage

  def unsafeRunAllTransactions[S](initialState: S, transactionExecutor: (S, Tx) => Option[S]): S = {
    logger.trace("Running all transactions")
    run(headPointer, StateSnapshot(initialState, TimeSlot.zero), transactionExecutor)
  }

  def unsafeRunTransactionsFromPreviousStateSnapshot[S](snapshot: StateSnapshot[S], transactionExecutor: (S, Tx) => Option[S]): S = {
    logger.trace("Running transactions from a given snapshot")
    run(headPointer, snapshot, transactionExecutor)
  }

  def runAllFinalizedTransactions[S](now: TimeSlot, initialState: S, transactionExecutor: (S, Tx) => Option[S]): S = {
    logger.trace("Running all finalized transactions")
    run(finalizedHeadPointer(now), StateSnapshot(initialState, TimeSlot.zero), transactionExecutor)
  }

  def runFinalizedTransactionsFromPreviousStateSnapshot[S](now: TimeSlot, snapshot: StateSnapshot[S], transactionExecutor: (S, Tx) => Option[S]): S = {
    logger.trace("Running finalized transactions from a given state snapshot")
    run(finalizedHeadPointer(now), snapshot, transactionExecutor)
  }

  private[blockchain] def run[S](lastBlockPointer: BlockPointer, snapshot: StateSnapshot[S], transactionExecutor: (S, Tx) => Option[S]): S = {
    val initialState = snapshot.computedState
    val snapshotTimestamp = snapshot.snapshotTimestamp
    def safeTransactionExecutor(previous: S, tx: Tx): S = {
      transactionExecutor(previous, tx) getOrElse previous
    }

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

    logger.debug("Testing chain segment to be added", "segment" -> chainSegment)
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
        logger.debug("Chain segment added to the chain")
        headPointer = BlockPointer(hash(chainSegment.mostRecentBlock.get), finalHeight)
        logger.debug(s"New head pointer assigned", "head pointer hash" -> headPointer.at, "height" -> headPointer.blockchainHeight)
      }
    }

    logger.debug("Chain segment processed", "segment" -> chainSegment)
  }

  def createBlockData(transactions: List[Tx], timeSlot: TimeSlot, key: SigningPrivateKey): Block[Tx] = {
    val body =
      BlockBody[Tx](
        hash(head),
        transactions,
        timeSlot,
        sign(timeSlot, key)
      )

    logger.debug(s"New block created", "block body" -> body)

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
    logger.trace("Retrieving block from storage", "block hash" -> id)
    if (genesisBlockHash == id) {
      logger.trace(s"Returning the genesis block", "block hash" -> id)
      Some(genesisBlock)
    } else {
      val response = storage.get(id)
      response match {
        case None => logger.trace("No block was found in storage", "block hash" -> id)
        case Some(b) => logger.trace("Returning non genesis block", "block hash" -> id, "block" -> b)
      }
      response
    }
  }

  def height: Height = headPointer.blockchainHeight
}

object Blockchain {

  object LoggingFormat {

    implicit val hashLoggable: Loggable[Hash] = Loggable.gen[Hash] { hash =>
      val wholeHash = hash.toCompactString()
      val firstSixChars = wholeHash.slice(1, 9)
      val lasttSixChars = wholeHash.takeRight(8)
      firstSixChars + " ... " + lasttSixChars
    }

    implicit val heightLoggable: Loggable[Height] = Loggable.gen[Height](_.toInt.toString)

    implicit val timeSlotLoggable: Loggable[TimeSlot] = Loggable.gen[TimeSlot](_.toLong.toString)

    implicit def blockBodyLoggable[Tx]: Loggable[BlockBody[Tx]] = Loggable.gen[BlockBody[Tx]] { bb =>
      List(
        s"previous hash -> ${hashLoggable.log(bb.previousHash)}",
        s"number of transactions -> ${bb.delta.length}",
        s"time slot -> ${timeSlotLoggable.log(bb.timeSlot)}"
      ).mkString(", ")
    }

    implicit def blockLoggable[Tx: Loggable]: Loggable[Block[Tx]] = Loggable.gen[Block[Tx]]{ b =>
      s"Block(${blockBodyLoggable.log(b.body)})"
    }

    implicit def chainSegmentLoggable[Tx: Loggable]: Loggable[ChainSegment[Tx]] = {
      Loggable.gen[ChainSegment[Tx]] { cs =>
        cs.blocks match {
          case Nil => "< empty-segment >"
          case List(b1) => s"< ${blockLoggable[Tx].log(b1)} >"
          case List(b1, b2) => s"< ${blockLoggable[Tx].log(b1)}, ${blockLoggable[Tx].log(b2)} >"
          case longerSegment =>
            val h = longerSegment.head
            val l = longerSegment.last
            s"< ${blockLoggable[Tx].log(h)} ,..., ${blockLoggable[Tx].log(l)} > (segment length = ${longerSegment.length})"
        }
      }
    }
  }

  def apply[Tx: Codec: Loggable](keys: List[SigningPublicKey], maxNumOfAdversaries: Int, database: String, segmentValidator: SegmentValidator): Blockchain[Tx] = {
    val storage = H2BlockStorage[Tx](database)
    new Blockchain[Tx](segmentValidator, storage)(keys, maxNumOfAdversaries)
  }
}

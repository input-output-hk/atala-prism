package io.iohk.node.cardano.repositories

import java.time.Instant

import doobie.implicits._
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.cardano.models._
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationLong
import scala.util.Random

class CardanoBlockRepositorySpec extends PostgresRepositorySpec {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)
  lazy val blockRepository = new CardanoBlockRepository(database)

  override def beforeAll(): Unit = {
    super.beforeAll()

    createTables()
  }

  /**
    * Creates a trimmed-down version of the {@code cexplorer} database structure.
    */
  private def createTables(): Unit = {
    sql"""
         |CREATE DOMAIN public.hash32type AS bytea
         |	CONSTRAINT hash32type_check CHECK ((octet_length(VALUE) = 32));
         |
         |CREATE DOMAIN public.uinteger AS integer
         |	CONSTRAINT uinteger_check CHECK ((VALUE >= 0));
         |
         |CREATE TABLE public.block (
         |    id serial,
         |    hash public.hash32type NOT NULL,
         |    block_no public.uinteger,
         |    previous bigint,
         |    "time" timestamp without time zone NOT NULL
         |);
         |
         |CREATE TABLE public.tx (
         |    id SERIAL,
         |    hash public.hash32type NOT NULL,
         |    block bigint NOT NULL
         |);
      """.stripMargin.update.run.transact(database).unsafeRunSync()
  }

  private def insertBlock(block: Block.Full): Unit = {
    // Queries are meant to ignore null block numbers, so we nullify them when they are zero
    val blockNoOption = if (block.header.blockNo == 0) None else Some(block.header.blockNo)
    sql"""
         |INSERT INTO block (hash, block_no, previous, time)
         |  VALUES (
         |    ${block.header.hash.toBytesBE},
         |    $blockNoOption,
         |    (SELECT id FROM block WHERE hash = ${block.header.previousBlockHash.map(_.toBytesBE)}),
         |    ${block.header.time})
    """.stripMargin.update.run.transact(database).unsafeRunSync()

    block.transactions.foreach(insertTransaction)
  }

  private def insertTransaction(transaction: Transaction): Unit = {
    sql"""
         |INSERT INTO tx (hash, block)
         |  VALUES (
         |    ${transaction.id.toBytesBE},
         |    (SELECT id FROM block WHERE hash = ${transaction.blockHash.toBytesBE}))
    """.stripMargin.update.run.transact(database).unsafeRunSync()
  }

  /**
    * Creates and returns the genesis block and {@code n} random blocks.
    */
  private def createRandomBlocks(n: Int): Seq[Block.Full] = {
    var previousBlockHash: Option[BlockHash] = None
    val genesisTime = Instant.now().minusSeconds(1000)
    0 to n map { blockNo =>
      val time = genesisTime.plusSeconds(20 * blockNo)
      val blockHash = randomBlockHash()
      val block = Block.Full(
        BlockHeader(blockHash, blockNo, time, previousBlockHash),
        createRandomTransactions(blockHash, blockNo).toList
      )
      previousBlockHash = Some(blockHash)
      block
    }
  }

  private def createRandomTransactions(blockHash: BlockHash, n: Int): Seq[Transaction] = {
    0 to n map { _ =>
      Transaction(randomTransactionId(), blockHash)
    }
  }

  "getBlock" should {
    "return the requested block" in {
      val blocks = createRandomBlocks(5)
      blocks.foreach(insertBlock)
      val toFindBlock = Block.Canonical(blocks(3).header)

      val result = blockRepository.getBlock(toFindBlock.header.hash).value.futureValue

      val block = result.right.value
      block must be(toFindBlock)
    }

    "return NotFound when the block is not found" in {
      createRandomBlocks(5).foreach(insertBlock)
      val blockHash = randomBlockHash()

      val result = blockRepository.getBlock(blockHash).value.futureValue

      val error = result.left.value
      error must be(BlockError.NotFound(blockHash))
    }

    "return NotFound for the genesis block" in {
      val blocks = createRandomBlocks(5)
      blocks.foreach(insertBlock)
      val blockHash = blocks.head.header.hash

      val result = blockRepository.getBlock(blockHash).value.futureValue

      val error = result.left.value
      error must be(BlockError.NotFound(blockHash))
    }
  }

  "getFullBlock" should {
    "return the requested block with all its transactions" in {
      val blocks = createRandomBlocks(5)
      blocks.foreach(insertBlock)
      val toFindBlock = blocks(3)

      val result = blockRepository.getFullBlock(toFindBlock.header.hash).value.futureValue

      val block = result.right.value
      block must be(toFindBlock)
    }

    "return NotFound when the block is not found" in {
      createRandomBlocks(5).foreach(insertBlock)
      val blockHash = randomBlockHash()

      val result = blockRepository.getFullBlock(blockHash).value.futureValue

      val error = result.left.value
      error must be(BlockError.NotFound(blockHash))
    }

    "return NotFound for the genesis block" in {
      val blocks = createRandomBlocks(5)
      blocks.foreach(insertBlock)
      val blockHash = blocks.head.header.hash

      val result = blockRepository.getFullBlock(blockHash).value.futureValue

      val error = result.left.value
      error must be(BlockError.NotFound(blockHash))
    }
  }

  "getLatestBlock" should {
    "return the latest block" in {
      val blocks = createRandomBlocks(5)
      blocks.foreach(insertBlock)
      val latestBlock = Block.Canonical(blocks.last.header)

      val result = blockRepository.getLatestBlock().value.futureValue

      val block = result.right.value
      block must be(latestBlock)
    }

    "return NoneAvailable when only the genesis block exist" in {
      val blocks = createRandomBlocks(0)
      blocks.foreach(insertBlock)

      val result = blockRepository.getLatestBlock().value.futureValue

      val error = result.left.value
      error must be(BlockError.NoneAvailable)
    }

    "return NoneAvailable when there are no blocks" in {
      val result = blockRepository.getLatestBlock().value.futureValue

      val error = result.left.value
      error must be(BlockError.NoneAvailable)
    }
  }

  def random32Bytes(): Array[Byte] = {
    val bytes = Array.ofDim[Byte](32)
    Random.nextBytes(bytes)
    bytes
  }

  def randomBlockHash(): BlockHash = {
    BlockHash.fromBytesBE(random32Bytes()).get
  }

  def randomTransactionId(): TransactionId = {
    TransactionId.fromBytesBE(random32Bytes()).get
  }
}

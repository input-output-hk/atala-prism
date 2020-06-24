package io.iohk.node.cardano.dbsync.repositories.testing

import java.time.Instant

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.utils.DoobieImplicits._
import io.iohk.node.cardano.models._

import scala.util.Random

object TestCardanoBlockRepository {

  /**
    * Creates a trimmed-down version of the {@code cexplorer} database structure.
    */
  def createTables()(implicit database: Transactor[IO]): Unit = {
    sql"""
         |CREATE DOMAIN public.hash32type AS bytea
         |	CONSTRAINT hash32type_check CHECK ((octet_length(VALUE) = 32));
         |
         |CREATE DOMAIN public.uinteger AS integer
         |	CONSTRAINT uinteger_check CHECK ((VALUE >= 0));
         |
         |CREATE TABLE public.block (
         |    id SERIAL,
         |    hash public.hash32type NOT NULL,
         |    block_no public.uinteger,
         |    previous BIGINT,
         |    "time" TIMESTAMP WITHOUT TIME ZONE NOT NULL
         |);
         |
         |CREATE TABLE public.tx (
         |    id SERIAL,
         |    hash public.hash32type NOT NULL,
         |    block bigint NOT NULL,
         |    block_index INT4 NOT NULL
         |);
      """.stripMargin.update.run.transact(database).unsafeRunSync()
    ()
  }

  def insertBlock(block: Block.Full)(implicit database: Transactor[IO]): Unit = {
    // Queries are meant to ignore null block numbers, so we nullify them when they are zero
    val blockNoOption = if (block.header.blockNo == 0) None else Some(block.header.blockNo)
    sql"""
         |INSERT INTO block (hash, block_no, previous, time)
         |  VALUES (
         |    ${block.header.hash.value},
         |    $blockNoOption,
         |    (SELECT id FROM block WHERE hash = ${block.header.previousBlockHash.map(_.value)}),
         |    ${block.header.time})
    """.stripMargin.update.run.transact(database).unsafeRunSync()

    block.transactions.zipWithIndex.foreach(insertTransaction _ tupled _)
  }

  private def insertTransaction(transaction: Transaction, blockIndex: Int)(implicit database: Transactor[IO]): Unit = {
    sql"""
         |INSERT INTO tx (hash, block, block_index)
         |  VALUES (
         |    ${transaction.id.value},
         |    (SELECT id FROM block WHERE hash = ${transaction.blockHash.value}),
         |    $blockIndex)
    """.stripMargin.update.run.transact(database).unsafeRunSync()
    ()
  }

  /**
    * Creates a genesis block and {@code n} other random blocks, for a total of {@code n+1} blocks.
    */
  def createRandomBlocks(n: Int): Seq[Block.Full] = {
    var previousBlockHash: Option[BlockHash] = None
    val genesisTime = Instant.now().minusSeconds(1000)
    // Create n+1 blocks, where the first block with index 0 is the genesis block
    for (blockNo <- 0 to n) yield {
      val time = genesisTime.plusSeconds(20L * blockNo)
      val blockHash = TestCardanoBlockRepository.randomBlockHash()
      val block = Block.Full(
        BlockHeader(blockHash, blockNo, time, previousBlockHash),
        createRandomTransactions(blockHash, blockNo).toList
      )
      previousBlockHash = Some(blockHash)
      block
    }
  }

  def createRandomTransactions(blockHash: BlockHash, n: Int): Seq[Transaction] = {
    0 to n map { _ =>
      Transaction(TestCardanoBlockRepository.randomTransactionId(), blockHash)
    }
  }

  private def random32Bytes(): Seq[Byte] = {
    val bytes = Array.ofDim[Byte](32)
    Random.nextBytes(bytes)
    bytes
  }

  private def randomBlockHash(): BlockHash = {
    BlockHash.from(random32Bytes()).get
  }

  private def randomTransactionId(): TransactionId = {
    TransactionId.from(random32Bytes()).get
  }
}

package io.iohk.atala.prism.node.cardano.dbsync.repositories.testing

import java.time.Instant
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import doobie.implicits.legacy.instant._
import doobie.postgres.circe.json.implicits.jsonPut
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.models._
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.utils.DoobieImplicits._

import scala.util.Random

object TestCardanoBlockRepository {

  /** Creates a trimmed-down version of the {@code cexplorer} database structure.
    */
  def createTables()(implicit database: Transactor[IO]): Unit = {
    sql"""
         |CREATE DOMAIN public.hash32type AS bytea
         |	CONSTRAINT hash32type_check CHECK ((octet_length(VALUE) = 32));
         |
         |CREATE DOMAIN public.uinteger AS integer
         |	CONSTRAINT uinteger_check CHECK ((VALUE >= 0));
         |
         |CREATE DOMAIN public.word64type AS numeric (20, 0)
         |  CHECK (VALUE >= 0 AND VALUE <= 18446744073709551615);
         |
         |CREATE TABLE public.block (
         |    id SERIAL,
         |    hash public.hash32type NOT NULL,
         |    block_no public.uinteger,
         |    previous_id BIGINT,
         |    "time" TIMESTAMP WITHOUT TIME ZONE NOT NULL
         |);
         |
         |CREATE TABLE public.tx (
         |    id SERIAL,
         |    hash public.hash32type NOT NULL,
         |    block_id bigint NOT NULL,
         |    block_index INT4 NOT NULL
         |);
         |
         |CREATE TABLE public.tx_metadata (
         |    id SERIAL8 PRIMARY KEY UNIQUE,
         |    key public.word64type NOT NULL,
         |    json JSONB NOT NULL,
         |    tx_id INT8 NOT NULL
         |);
      """.stripMargin.update.run.transact(database).unsafeRunSync()
    ()
  }

  def insertBlock(
      block: Block.Full
  )(implicit database: Transactor[IO]): Unit = {
    // Queries are meant to ignore null block numbers, so we nullify them when they are zero
    val blockNoOption =
      if (block.header.blockNo == 0) None else Some(block.header.blockNo)
    sql"""
         |INSERT INTO block (hash, block_no, previous_id, time)
         |  VALUES (
         |    ${block.header.hash.value},
         |    $blockNoOption,
         |    (SELECT id FROM block WHERE hash = ${block.header.previousBlockHash
      .map(_.value)}),
         |    ${block.header.time})
    """.stripMargin.update.run.transact(database).unsafeRunSync()

    block.transactions.zipWithIndex.foreach(insertTransaction _ tupled _)
  }

  def insertTransaction(transaction: Transaction, blockIndex: Int)(implicit
      database: Transactor[IO]
  ): Unit = {
    // Insert the transaction
    sql"""
         |INSERT INTO tx (hash, block_id, block_index)
         |  VALUES (
         |    ${transaction.id.value},
         |    (SELECT id FROM block WHERE hash = ${transaction.blockHash.value}),
         |    $blockIndex)
    """.stripMargin.update.run.transact(database).unsafeRunSync()

    // Insert the metadata
    for {
      metadata <- transaction.metadata
      json <- metadata.json.asObject
      _ = json.toMap.foreach { case (key, value) =>
        sql"""
               |INSERT INTO tx_metadata (key, json, tx_id)
               |  VALUES (
               |    ${key.toInt},
               |    $value,
               |    (SELECT id FROM tx WHERE hash = ${transaction.id.value}))
          """.stripMargin.update.run.transact(database).unsafeRunSync()
      }
    } yield ()

    ()
  }

  /** Creates a genesis block and {@code n} other random blocks, for a total of {@code n+1} blocks.
    */
  def createRandomBlocks(n: Int): Seq[Block.Full] = {
    var previousBlock: Option[Block.Full] = None
    // Create n+1 blocks, where the first block with index 0 is the genesis block
    0 to n map { _ =>
      val block = createNextRandomBlock(previousBlock)
      previousBlock = Some(block)
      block
    }
  }

  def createNextRandomBlock(previousBlock: Option[Block.Full]): Block.Full = {
    val blockNo = previousBlock.map(_.header.blockNo + 1).getOrElse(0)
    val time =
      previousBlock.map(_.header.time).getOrElse(Instant.now()).plusSeconds(20)
    val blockHash = TestCardanoBlockRepository.randomBlockHash()
    val block = Block.Full(
      BlockHeader(blockHash, blockNo, time, previousBlock.map(_.header.hash)),
      createRandomTransactions(blockHash, 5).toList
    )
    block
  }

  def createRandomTransactions(
      blockHash: BlockHash,
      n: Int
  ): Seq[Transaction] = {
    0 to n map { blockIndex =>
      Transaction(
        TestCardanoBlockRepository.randomTransactionId(),
        blockHash,
        blockIndex,
        None
      )
    }
  }

  def random32Bytes(): Array[Byte] = {
    val bytes = Array.ofDim[Byte](32)
    Random.nextBytes(bytes)
    bytes
  }

  private def randomBlockHash(): BlockHash = {
    BlockHash.from(random32Bytes()).get
  }

  def randomTransactionId(): TransactionId = {
    TransactionId.from(random32Bytes()).get
  }
}

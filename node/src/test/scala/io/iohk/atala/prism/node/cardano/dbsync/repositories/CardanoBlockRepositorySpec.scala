package io.iohk.atala.prism.node.cardano.dbsync.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.scalatest.EitherMatchers._
import io.circe.Json
import io.iohk.atala.prism.node.AtalaWithPostgresSpec
import io.iohk.atala.prism.node.cardano.dbsync.repositories.testing.TestCardanoBlockRepository
import io.iohk.atala.prism.node.cardano.models._
import tofu.logging.Logging.Make
import tofu.logging.Logging

class CardanoBlockRepositorySpec extends AtalaWithPostgresSpec {
  val logs: Make[IO] = Logging.Make.plain[IO]
  lazy val blockRepository: CardanoBlockRepository[IO] =
    CardanoBlockRepository(database, logs)

  override def beforeAll(): Unit = {
    super.beforeAll()

    TestCardanoBlockRepository.createTables()
  }

  "getFullBlock" should {
    "return the requested block with all its transactions" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(5)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)
      val toFindBlock = blocks(3)

      val result =
        blockRepository.getFullBlock(toFindBlock.header.blockNo).unsafeRunSync()

      result must beRight(toFindBlock)
    }

    "return transactions without metadata" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(1)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)
      val block = blocks(1)
      val transaction =
        Transaction(
          id = TestCardanoBlockRepository.randomTransactionId(),
          blockHash = block.header.hash,
          blockIndex = block.transactions.size,
          metadata = None
        )
      TestCardanoBlockRepository.insertTransaction(
        transaction,
        block.transactions.size
      )

      val result =
        blockRepository.getFullBlock(block.header.blockNo).unsafeRunSync()

      result.map(_.transactions.last) must beRight(transaction)
    }

    "return transactions without non-PRISM metadata" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(1)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)
      val block = blocks(1)
      val transaction = Transaction(
        id = TestCardanoBlockRepository.randomTransactionId(),
        blockHash = block.header.hash,
        blockIndex = block.transactions.size,
        metadata = Some(
          TransactionMetadata(
            Json.obj("1" -> Json.obj("is_this_prism" -> Json.fromString("no")))
          )
        )
      )
      TestCardanoBlockRepository.insertTransaction(
        transaction,
        block.transactions.size
      )
      val transactionWithoutMetadata = transaction.copy(metadata = None)

      val result =
        blockRepository.getFullBlock(block.header.blockNo).unsafeRunSync()

      result.map(_.transactions.last) must beRight(transactionWithoutMetadata)
    }

    "return transactions with PRISM metadata" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(1)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)
      val block = blocks(1)
      val transaction = Transaction(
        id = TestCardanoBlockRepository.randomTransactionId(),
        blockHash = block.header.hash,
        blockIndex = block.transactions.size,
        metadata = Some(
          TransactionMetadata(
            Json.obj(
              AtalaObjectMetadata.METADATA_PRISM_INDEX.toString -> Json.obj(
                "is_this_prism" -> Json.fromString("yes")
              )
            )
          )
        )
      )
      TestCardanoBlockRepository.insertTransaction(
        transaction,
        block.transactions.size
      )

      val result =
        blockRepository.getFullBlock(block.header.blockNo).unsafeRunSync()

      result.map(_.transactions.last) must beRight(transaction)
    }

    "return NotFound when the block is not found" in {
      TestCardanoBlockRepository
        .createRandomBlocks(5)
        .foreach(TestCardanoBlockRepository.insertBlock)
      val blockNo = 1337

      val result = blockRepository.getFullBlock(blockNo).unsafeRunSync()

      result must beLeft(BlockError.NotFound(blockNo))
    }

    "return NotFound for the genesis block" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(5)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)
      val blockNo = blocks.head.header.blockNo

      val result = blockRepository.getFullBlock(blockNo).unsafeRunSync()

      result must beLeft(BlockError.NotFound(blockNo))
    }
  }

  "getLatestBlock" should {
    "return the latest block" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(5)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)
      val latestBlock = Block.Canonical(blocks.last.header)

      val result = blockRepository.getLatestBlock.unsafeRunSync()

      result must beRight(latestBlock)
    }

    "return NoneAvailable when only the genesis block exist" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(0)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)

      val result = blockRepository.getLatestBlock.unsafeRunSync()

      result must beLeft(BlockError.NoneAvailable)
    }

    "return NoneAvailable when there are no blocks" in {
      val result = blockRepository.getLatestBlock.unsafeRunSync()

      result must beLeft(BlockError.NoneAvailable)
    }
  }
}

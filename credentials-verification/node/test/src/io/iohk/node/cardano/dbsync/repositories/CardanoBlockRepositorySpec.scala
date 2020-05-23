package io.iohk.node.cardano.dbsync.repositories

import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.cardano.dbsync.repositories.testing.TestCardanoBlockRepository
import io.iohk.node.cardano.models._
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationLong

class CardanoBlockRepositorySpec extends PostgresRepositorySpec {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)
  lazy val blockRepository = new CardanoBlockRepository(database)

  override def beforeAll(): Unit = {
    super.beforeAll()

    TestCardanoBlockRepository.createTables()
  }

  "getFullBlock" should {
    "return the requested block with all its transactions" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(5)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)
      val toFindBlock = blocks(3)

      val result = blockRepository.getFullBlock(toFindBlock.header.blockNo).value.futureValue

      val block = result.right.value
      block must be(toFindBlock)
    }

    "return NotFound when the block is not found" in {
      TestCardanoBlockRepository.createRandomBlocks(5).foreach(TestCardanoBlockRepository.insertBlock)
      val blockNo = 1337

      val result = blockRepository.getFullBlock(blockNo).value.futureValue

      val error = result.left.value
      error must be(BlockError.NotFound(blockNo))
    }

    "return NotFound for the genesis block" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(5)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)
      val blockNo = blocks.head.header.blockNo

      val result = blockRepository.getFullBlock(blockNo).value.futureValue

      val error = result.left.value
      error must be(BlockError.NotFound(blockNo))
    }
  }

  "getLatestBlock" should {
    "return the latest block" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(5)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)
      val latestBlock = Block.Canonical(blocks.last.header)

      val result = blockRepository.getLatestBlock().value.futureValue

      val block = result.right.value
      block must be(latestBlock)
    }

    "return NoneAvailable when only the genesis block exist" in {
      val blocks = TestCardanoBlockRepository.createRandomBlocks(0)
      blocks.foreach(TestCardanoBlockRepository.insertBlock)

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
}

package io.iohk.node.repositories.blocks

import io.iohk.node.bitcoin.models.{Block, BlockError, Blockhash}
import io.iohk.node.repositories.common.PostgresRepositorySpec
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationLong
import scala.util.Random

class BlocksRepositorySpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)
  lazy val blocksRepository = new BlocksRepository(database)

  "create" should {
    "work" in {
      val block = randomBlock()
      blocksRepository.create(block).futureValue
      succeed
    }
  }

  "find" should {
    "find a block" in {
      val block = randomBlock()
      blocksRepository.create(block).futureValue
      val result = blocksRepository.find(block.hash).futureValue
      result.right.value must be(block)
    }
  }

  "getLatest" should {
    "work" in {
      val a = randomBlock().copy(height = 1)
      val b = randomBlock().copy(height = 2)
      List(a, b).foreach(blocksRepository.create(_).futureValue)

      val result = blocksRepository.getLatest.futureValue
      result.right.value must be(b)
    }
  }

  "removeLatest" should {
    "work" in {
      val a = randomBlock().copy(height = 1)
      val b = randomBlock().copy(height = 2)
      List(a, b).foreach(blocksRepository.create(_).futureValue)

      val result = blocksRepository.removeLatest().futureValue
      result.right.value must be(b)
      blocksRepository.find(b.hash).futureValue must be(Left(BlockError.NotFound(b.hash)))
      blocksRepository.getLatest.futureValue.right.value must be(a)
    }
  }

  def randomBlock(): Block = {
    val bytes = Array.ofDim[Byte](32)
    Random.nextBytes(bytes)
    Block(Blockhash.fromBytesBE(bytes).value, Random.nextInt(100), Random.nextLong(), None)
  }
}

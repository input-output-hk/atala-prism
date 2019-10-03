package io.iohk.node.repositories.blocks

import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.bitcoin.models.{BlockError, BlockHeader, Blockhash}
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.Random

class BlocksRepositorySpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)
  lazy val blocksRepository = new BlocksRepository(database)

  "create" should {
    "work" in {
      Future.successful(()).futureValue
      val block = randomBlock()
      blocksRepository.create(block).value.futureValue
      succeed
    }
  }

  "find" should {
    "find a block" in {
      val block = randomBlock()
      blocksRepository.create(block).value.futureValue
      val result = blocksRepository.find(block.hash).value.futureValue
      result.right.value must be(block)
    }
  }

  "getLatest" should {
    "work" in {
      val a = randomBlock().copy(height = 1)
      val b = randomBlock().copy(height = 2)
      List(a, b).foreach(blocksRepository.create(_).value.futureValue)

      val result = blocksRepository.getLatest.value.futureValue
      result.right.value must be(b)
    }
  }

  "removeLatest" should {
    "work" in {
      val a = randomBlock().copy(height = 1)
      val b = randomBlock().copy(height = 2)
      List(a, b).foreach(blocksRepository.create(_).value.futureValue)

      val result = blocksRepository.removeLatest().value.futureValue
      result.right.value must be(b)
      blocksRepository.find(b.hash).value.futureValue must be(Left(BlockError.NotFound(b.hash)))
      blocksRepository.getLatest.value.futureValue.right.value must be(a)
    }
  }

  def randomBlock(): BlockHeader = {
    val bytes = Array.ofDim[Byte](32)
    Random.nextBytes(bytes)
    BlockHeader(Blockhash.fromBytesBE(bytes).value, Random.nextInt(100), Random.nextLong(), None)
  }
}

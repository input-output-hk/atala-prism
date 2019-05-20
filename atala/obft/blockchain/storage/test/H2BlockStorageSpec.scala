package atala.obft.blockchain.storage
package test

import atala.clock.TimeSlot
import atala.obft.blockchain.models._
import io.iohk.decco.auto._
import io.iohk.multicrypto._
import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.{BeforeAndAfter, WordSpec}

class H2BlockStorageSpec extends WordSpec with BeforeAndAfter {

  import H2BlockStorageSpec._

  val genesis = GenesisBlock[Tx](List(generateSigningKeyPair().public, generateSigningKeyPair().public))
  val dummySignature = sign(genesis, generateSigningKeyPair().`private`)

  val body = BlockBody(
    previousHash = hash(genesis),
    delta = List("a", "b", "c"),
    timeSlot = TimeSlot.from(1).value,
    timeSlotSignature = dummySignature
  )

  val block = Block(
    body = body,
    signature = dummySignature
  )

  val tempPath = java.nio.file.Files.createTempFile("test", "iohk")
  val service = H2BlockStorage[Tx]("h2db")

  val latestBlock = {
    val nextTimeSlot = block.body.timeSlot.next
    block.copy(body = block.body.copy(timeSlot = nextTimeSlot))
  }

  val modifiedHash: Hash = hash(latestBlock)

  before {
    service.remove(hash(block))
    service.remove(hash(latestBlock))
  }

  "put" should {
    "create a block" in {
      service.put(hash(block), block)

      val retrieved = service.get(hash(block))
      retrieved.value must be(block)
    }

    "fail to create an existing block" in {
      service.put(hash(block), block)

      intercept[Exception] {
        service.put(hash(block), block)
      }
    }

    "fail to create a block if its time slot is already stored" in {
      service.put(hash(block), block)

      hash(block) must not be modifiedHash

      intercept[Exception] {
        service.put(hash(modifiedHash), block)
      }
    }
  }

  "get" should {
    "return an existing item" in {
      service.put(hash(block), block)

      val retrieved = service.get(hash(block))
      retrieved.value must be(block)
    }

    "return none when the item doesn't exists" in {
      val unknownGenesis = GenesisBlock[Tx](List(generateSigningKeyPair().public))

      val result = service.get(hash(unknownGenesis))
      result must be(empty)
    }
  }

  "getLatestBlock" should {
    "return None on an empty storage" in {
      service.getLatestBlock() mustBe empty
    }

    "return the block with latest time slot" in {
      service.put(hash(block), block)
      service.put(hash(latestBlock), latestBlock)
      service.getLatestBlock() mustBe Some(latestBlock)
    }
  }

  "getNumberOfBlocks" should {
    "return 0 on an empty storage" in {
      service.getNumberOfBlocks() mustBe 0
    }

    "return the correct count of blocks on a non empty storage" in {
      service.put(hash(block), block)
      service.put(hash(latestBlock), latestBlock)
      service.getNumberOfBlocks() mustBe 2
    }
  }

  "remove" should {
    "work when the item is not present" in {
      val unknownGenesis = GenesisBlock[Tx](List(generateSigningKeyPair().public))

      service.remove(hash(unknownGenesis))
    }

    "remove an existing item" in {
      service.put(hash(block), block)
      service.remove(hash(block))

      val retrieved = service.get(hash(block))
      retrieved must be(empty)
    }
  }

  "update" should {
    "do nothing with empty parameters" in {
      service.put(hash(block), block)
      service.getNumberOfBlocks() mustBe 1
      service.update(Nil, Nil)
      service.getNumberOfBlocks() mustBe 1
      service.get(hash(block)) mustBe Some(block)
    }

    "accept empty list of inserts and remove specified blocks" in {
      service.put(hash(block), block)
      service.put(hash(latestBlock), latestBlock)
      service.getNumberOfBlocks() mustBe 2

      service.update(List(hash(block), hash(latestBlock)), Nil)
      service.getNumberOfBlocks() mustBe 0
      service.get(hash(block)) mustBe empty
      service.get(hash(latestBlock)) mustBe empty
    }

    "accept empty list of deletes and add specified blocks" in {
      service.getNumberOfBlocks() mustBe 0

      service.update(Nil, List((hash(block), block), (hash(latestBlock), latestBlock)))
      service.getNumberOfBlocks() mustBe 2
      service.get(hash(block)) mustBe Some(block)
      service.get(hash(latestBlock)) mustBe Some(latestBlock)
    }

    "add and remove one block on a valid situation" in {
      service.put(hash(block), block)

      service.update(List(hash(block)), List((hash(latestBlock), latestBlock)))

      service.getNumberOfBlocks() mustBe 1
      service.get(hash(latestBlock)) mustBe Some(latestBlock)
    }

    "not modify the database if the insertion fails" in {
      service.put(hash(block), block)

      intercept[Exception] {
        service.update(
          List(hash(block)),
          List(
            (hash(latestBlock), latestBlock),
            (hash(latestBlock), latestBlock) //this invalid repetition should be stopped by the db
          )
        )
      }

      service.getNumberOfBlocks() mustBe 1
      service.get(hash(block)) mustBe Some(block)
      service.get(hash(latestBlock)) mustBe empty
    }

  }
}

object H2BlockStorageSpec {

  type Tx = String

}

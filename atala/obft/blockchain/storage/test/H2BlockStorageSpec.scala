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
    timeSlot = TimeSlot(1),
    timeSlotSignature = dummySignature
  )

  val block = Block(
    body = body,
    signature = dummySignature
  )

  val tempPath = java.nio.file.Files.createTempFile("test", "iohk")
  val service = H2BlockStorage[Tx]("h2db")

  before {
    service.remove(hash(block))
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
}

object H2BlockStorageSpec {

  type Tx = String

}

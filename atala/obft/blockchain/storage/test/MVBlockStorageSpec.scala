package atala.obft.blockchain.storage
package test

import io.iohk.decco.auto._
import io.iohk.multicrypto._
import atala.obft.blockchain.models._
import atala.clock.TimeSlot
import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.{BeforeAndAfter, WordSpec}

class MVBlockStorageSpec extends WordSpec with BeforeAndAfter {

  import MVBlockStorageSpec._

  val genesis = GenesisBlock[Tx](List(generateSigningKeyPair().public, generateSigningKeyPair().public))
  val dummySignature = sign(genesis, generateSigningKeyPair().`private`)

  val body = BlockBody(
    hash = hash(genesis),
    delta = List("a", "b", "c"),
    timeSlot = TimeSlot(1),
    timeSlotSignature = dummySignature
  )

  val block = Block(
    body = body,
    signature = dummySignature
  )

  val tempPath = java.nio.file.Files.createTempFile("test", "iohk")
  val service = new MVBlockStorage[Tx](tempPath)

  before {
    service.remove(hash(block))
  }

  "put" should {
    "create a block" in {
      service.put(hash(block), block)

      val retrieved = service.get(hash(block))
      retrieved.value must be(block)
    }

    "replace an existing item" in {
      service.put(hash(block), block)

      val retrieved = service.get(hash(block))
      retrieved.value must be(block)
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

object MVBlockStorageSpec {

  type Tx = String

}

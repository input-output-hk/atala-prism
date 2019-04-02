package obft.blockchain
package test

// format: off

import obft.fakes._
import obft.clock._

import org.scalatest.WordSpec
import org.scalatest.MustMatchers

class BlockchainSpec extends WordSpec with MustMatchers {

  "Blockchain.createBlockData" should {

    "create a valid Block when all the parameters are correct" in {
      // GIVEN
      val blockchain = Blockchain[String](publicKeys, 1)

      // WHEN
      val block = blockchain.createBlockData(List("A", "B"), TimeSlot(3), keyPair1.PrivateKey)

      // THEN
      block.body.delta mustBe List("A", "B")
      block.body.hash mustBe Hash(GenesisBlock[String](publicKeys))
      block.body.timeSlot mustBe TimeSlot(3)
      block.body.timeSlotSignature mustBe Signature.sign(TimeSlot(3), keyPair1.PrivateKey)
      block.signature mustBe Signature.sign(BlockBody(Hash(genesisBlock), List("A", "B"), TimeSlot(3), Signature.sign(TimeSlot(3), keyPair1.PrivateKey)), keyPair1.PrivateKey)
    }

  }

  "Blockchain.add" should {

    "do nothing when the segment is empty" in {
      // GIVEN
      val blockchain = Blockchain[String](publicKeys, 1)

      //WHEN
      blockchain.add(Nil)

      //THEN
      blockchain.storage.data mustBe Map(Hash(genesisBlock) -> genesisBlock)
      blockchain.headPointer mustBe blockchain.BlockPointer(Hash(genesisBlock), 0)
    }

    "add a block, when the segment contains just that block (and it's valid)" in {
      // GIVEN
      val blockchain = Blockchain[String](publicKeys, 1)
      val body = BlockBody(Hash(genesisBlock), List("A", "B"), TimeSlot(3), Signature.sign(TimeSlot(3), keyPair1.PrivateKey))
      val block = Block(body, Signature.sign(body, keyPair1.PrivateKey))

      //WHEN
      blockchain.add(block :: Nil)

      //THEN
      blockchain.storage.data mustBe Map(Hash(genesisBlock) -> genesisBlock, Hash(block) -> block)
      blockchain.headPointer mustBe blockchain.BlockPointer(Hash(block), 1)
    }

    "add all the blocks of the segment, when the whole segment is valid" in {
      // GIVEN
      val blockchain = Blockchain[String](publicKeys, 1)
      def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
        val keyPair = keys((ts - 1) % keys.length)
        val key = keyPair.PrivateKey
        val body = BlockBody(Hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), Signature.sign(TimeSlot(ts), key))
        Block(body, Signature.sign(body, key))
      }
      val b1 = block(3, genesisBlock)
      val b2 = block(4, b1)
      val b3 = block(7, b2)
      val segment = b3 :: b2 :: b1 :: Nil

      //WHEN
      blockchain.add(segment)

      //THEN
      val targetBlockchain = segment ++ List(genesisBlock)
      val targetStorage = targetBlockchain.map(b => Hash(b) -> b).toMap
      blockchain.storage.data.size mustBe targetStorage.size
      blockchain.storage.data mustBe targetStorage
      blockchain.headPointer mustBe blockchain.BlockPointer(Hash(b3), 3)
    }

    "change nothing, when the segment contains just one block (and it's invalid)" in {
      // GIVEN
      val blockchain = Blockchain[String](publicKeys, 1)
      val body = BlockBody(Hash(genesisBlock), List("A", "B"), TimeSlot(3), Signature.sign(TimeSlot(5), keyPair1.PrivateKey))
      val block = Block(body, Signature.sign(body, keyPair1.PrivateKey))

      //WHEN
      blockchain.add(block :: Nil)

      //THEN
      blockchain.storage.data mustBe Map(Hash(genesisBlock) -> genesisBlock)
      blockchain.headPointer mustBe blockchain.BlockPointer(Hash(genesisBlock), 0)
    }

  }

  "Blockchain.*run*" should {

    "use all transactions when invoking unsafeRunAllTransactions" in {
      // GIVEN
      val blockchain = Blockchain[String](publicKeys, 1)
      def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
        val keyPair = keys((ts - 1) % keys.length)
        val key = keyPair.PrivateKey
        val body = BlockBody(Hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), Signature.sign(TimeSlot(ts), key))
        Block(body, Signature.sign(body, key))
      }
      val b1 = block(3, genesisBlock)
      val b2 = block(4, b1)
      val b3 = block(7, b2)
      val segment = b3 :: b2 :: b1 :: Nil

      blockchain.add(segment)

      //WHEN
      val result = blockchain.unsafeRunAllTransactions[List[String]](Nil, (a, s) => Some(s :: a))

      //THEN
      result mustBe List("B7", "A7", "B4", "A4", "B3", "A3")
    }

    "use only finalized transactions when invoking runAllFinalizedTransactions" in {
      // GIVEN
      val blockchain = Blockchain[String](publicKeys, 1)
      def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
        val keyPair = keys((ts - 1) % keys.length)
        val key = keyPair.PrivateKey
        val body = BlockBody(Hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), Signature.sign(TimeSlot(ts), key))
        Block(body, Signature.sign(body, key))
      }
      val b1 = block(3, genesisBlock)
      val b2 = block(4, b1)
      val b3 = block(7, b2)
      val segment = b3 :: b2 :: b1 :: Nil

      blockchain.add(segment)

      //WHEN
      val result = blockchain.runAllFinalizedTransactions[List[String]](TimeSlot(8), Nil, (a, s) => Some(s :: a))

      //THEN
      result mustBe List("B3", "A3")
    }

    "use all transactions from after the snapshot when invoking unsafeRunTransactionsFromPreviousStateSnapshot" in {
      // GIVEN
      val blockchain = Blockchain[String](publicKeys, 1)
      def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
        val keyPair = keys((ts - 1) % keys.length)
        val key = keyPair.PrivateKey
        val body = BlockBody(Hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), Signature.sign(TimeSlot(ts), key))
        Block(body, Signature.sign(body, key))
      }
      val b1 = block(3, genesisBlock)
      val b2 = block(4, b1)
      val b3 = block(7, b2)
      val segment = b3 :: b2 :: b1 :: Nil
      val snapshot = StateSnapshot[List[String]](List("SomePreviousState"), TimeSlot(3))

      blockchain.add(segment)

      //WHEN
      val result = blockchain.unsafeRunTransactionsFromPreviousStateSnapshot[List[String]](snapshot, (a, s) => Some(s :: a))

      //THEN
      result mustBe List("B7", "A7", "B4", "A4", "SomePreviousState")
    }

    "use only finalized transactions from after the snapshot when invoking runFinalizedTransactionsFromPreviousStateSnapshot" in {
      // GIVEN
      val blockchain = Blockchain[String](publicKeys, 1)
      def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
        val keyPair = keys((ts - 1) % keys.length)
        val key = keyPair.PrivateKey
        val body = BlockBody(Hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), Signature.sign(TimeSlot(ts), key))
        Block(body, Signature.sign(body, key))
      }
      val b1 = block(3, genesisBlock)
      val b2 = block(4, b1)
      val b3 = block(7, b2)
      val segment = b3 :: b2 :: b1 :: Nil
      val snapshot = StateSnapshot[List[String]](List("SomePreviousState"), TimeSlot(3))

      blockchain.add(segment)

      //WHEN
      val result = blockchain.runFinalizedTransactionsFromPreviousStateSnapshot[List[String]](TimeSlot(9), snapshot, (a, s) => Some(s :: a))

      //THEN
      result mustBe List("B4", "A4", "SomePreviousState")
    }

  }

  private val keyPair1 = KeyPair.gen()
  private val keyPair2 = KeyPair.gen()
  private val keys = List(keyPair1, keyPair2)
  private val publicKeys = keys.map(_.PublicKey)
  private val genesisBlock = GenesisBlock[String](publicKeys)
}

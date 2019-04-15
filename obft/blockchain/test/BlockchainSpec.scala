package obft.blockchain
package test

// format: off

import io.iohk.decco._
import io.iohk.decco.auto._
import io.iohk.decco.test.utils.CodecTestingHelpers._
import io.iohk.multicrypto._
import io.iohk.multicrypto.encoding.implicits._
import io.iohk.multicrypto.test.utils.CryptoEntityArbitraries
import obft.blockchain.storage.InMemoryBlockStorage
import obft.clock._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class BlockchainSpec extends WordSpec with MustMatchers with CryptoEntityArbitraries {

  implicit def genesisBlockArbitrary[T: Arbitrary]: Arbitrary[GenesisBlock[T]] =
    Arbitrary(
      for {
        ks <- arbitrary[List[SigningPublicKey]]
      } yield GenesisBlock[T](ks)
    )

  implicit val timeSlotArbitrary: Arbitrary[TimeSlot] =
    Arbitrary(arbitrary[Int].map(TimeSlot.apply))

  implicit def blockBodyArbitrary[T: Arbitrary]: Arbitrary[BlockBody[T]] =
    Arbitrary(
      for {
        h <- arbitrary[Hash]
        d <- arbitrary[List[T]]
        t <- arbitrary[TimeSlot]
        s <- arbitrary[Signature]
      } yield BlockBody[T](h, d, t, s)
    )

  implicit def blockArbitrary[T: Arbitrary]: Arbitrary[Block[T]] =
    Arbitrary(
      for {
        b <- arbitrary[BlockBody[T]]
        s <- arbitrary[Signature]
      } yield Block(b, s)
    )

  implicit def anyBlockArbitrary[T: Arbitrary]: Arbitrary[AnyBlock[T]] =
    Arbitrary(Gen.oneOf(arbitrary[Block[T]], arbitrary[GenesisBlock[T]]))

  "The encoders and crypto functions" should {
    "work with OBFT entities" in {
      testCodec[GenesisBlock[String]]
      testCodec[TimeSlot]
      testCodec[BlockBody[String]]
      testCodec[Block[String]]
      testCodec[AnyBlock[String]]
      val kp = generateSigningKeyPair()

      forAll{(b: AnyBlock[String]) =>
        val h = hash(b)
        val encoded = Codec[Hash].encode(h)
        Codec[Hash].decode(encoded) mustBe Right(h)
        val signature = sign(b, kp.`private`)
        isValidSignature(b, signature, kp.public) mustBe true
        isValidSignature(h, signature, kp.public) mustBe false
      }

      forAll{(ts: TimeSlot) =>
        val signature = sign(ts, kp.`private`)
        isValidSignature(ts, signature, kp.public) mustBe true
      }
    }
  }

  "Blockchain.createBlockData" should {

    "create a valid Block when all the parameters are correct" in {
      // GIVEN
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), new InMemoryBlockStorage)(publicKeys, 1)

      // WHEN
      val block = blockchain.createBlockData(List("A", "B"), TimeSlot(3), keyPair1.`private`)

      // THEN
      block.body.delta mustBe List("A", "B")
      block.body.hash mustBe hash(genesisBlock)
      block.body.timeSlot mustBe TimeSlot(3)
      block.body.timeSlotSignature mustBe sign(TimeSlot(3), keyPair1.`private`)
      block.signature mustBe sign(BlockBody(hash(genesisBlock), List("A", "B"), TimeSlot(3), sign(TimeSlot(3), keyPair1.`private`)), keyPair1.`private`)
    }

  }

  "Blockchain.add" should {

    "do nothing when the segment is empty" in {
      // GIVEN
      val storage = new InMemoryBlockStorage[String]
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), storage)(publicKeys, 1)

      //WHEN
      blockchain.add(Nil)

      //THEN
      storage.data mustBe Map(hash(genesisBlock) -> genesisBlock)
      blockchain.headPointer mustBe blockchain.BlockPointer(hash(genesisBlock), 0)
    }

    "add a block, when the segment contains just that block (and it's valid)" in {
      // GIVEN
      val storage = new InMemoryBlockStorage[String]
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), storage)(publicKeys, 1)

      val body = BlockBody(hash(genesisBlock), List("A", "B"), TimeSlot(3), sign(TimeSlot(3), keyPair1.`private`))
      val block = Block(body, sign(body, keyPair1.`private`))

      //WHEN
      blockchain.add(block :: Nil)

      //THEN
      storage.data mustBe Map(hash(genesisBlock) -> genesisBlock, hash(block: AnyBlock[String]) -> block)
      blockchain.headPointer mustBe blockchain.BlockPointer(hash(block: AnyBlock[String]), 1)
    }


    "add all the blocks of the segment, when the whole segment is valid" in {
      // GIVEN
      val storage = new InMemoryBlockStorage[String]
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), storage)(publicKeys, 1)

      def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
        val keyPair = keys((ts - 1) % keys.length)
        val key = keyPair.`private`
        val body = BlockBody(hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), sign(TimeSlot(ts), key))
        Block(body, sign(body, key))
      }
      val b1 = block(3, genesisBlock)
      val b2 = block(4, b1)
      val b3 = block(7, b2)
      val segment = b3 :: b2 :: b1 :: Nil

      //WHEN
      blockchain.add(segment)

      //THEN
      val targetBlockchain = segment ++ List(genesisBlock)
      val targetStorage = targetBlockchain.map(b => hash(b : AnyBlock[String]) -> b).toMap
      storage.data.size mustBe targetStorage.size
      storage.data mustBe targetStorage
      blockchain.headPointer mustBe blockchain.BlockPointer(hash(b3), 3)
    }

    "change nothing, when the segment contains just one block (and it's invalid)" in {
      // GIVEN
      val storage = new InMemoryBlockStorage[String]
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), storage)(publicKeys, 1)

      val body = BlockBody(hash(genesisBlock), List("A", "B"), TimeSlot(3), sign(TimeSlot(5), keyPair1.`private`))
      val block = Block(body, sign(body, keyPair1.`private`))

      //WHEN
      blockchain.add(block :: Nil)

      //THEN
      storage.data mustBe Map(hash(genesisBlock) -> genesisBlock)
      blockchain.headPointer mustBe blockchain.BlockPointer(hash(genesisBlock), 0)
    }

   }

  "Blockchain.*run*" should {

    "use all transactions when invoking unsafeRunAllTransactions" in {
      // GIVEN
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), new InMemoryBlockStorage)(publicKeys, 1)

      def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
        val keyPair = keys((ts - 1) % keys.length)
        val key = keyPair.`private`
        val body = BlockBody(hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), sign(TimeSlot(ts), key))
        Block(body, sign(body, key))
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
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), new InMemoryBlockStorage)(publicKeys, 1)
      def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
        val keyPair = keys((ts - 1) % keys.length)
        val key = keyPair.`private`
        val body = BlockBody(hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), sign(TimeSlot(ts), key))
        Block(body, sign(body, key))
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
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), new InMemoryBlockStorage)(publicKeys, 1)
      def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
        val keyPair = keys((ts - 1) % keys.length)
        val key = keyPair.`private`
        val body = BlockBody(hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), sign(TimeSlot(ts), key))
        Block(body, sign(body, key))
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
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), new InMemoryBlockStorage)(publicKeys, 1)
      def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
        val keyPair = keys((ts - 1) % keys.length)
        val key = keyPair.`private`
        val body = BlockBody(hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), sign(TimeSlot(ts), key))
        Block(body, sign(body, key))
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

  private val keyPair1 = generateSigningKeyPair()
  private val keyPair2 = generateSigningKeyPair()
  private val keys = List(keyPair1, keyPair2)
  private val publicKeys = keys.map(_.public)
  private val genesisBlock: AnyBlock[String] = GenesisBlock[String](publicKeys)
}

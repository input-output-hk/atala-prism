package atala.obft.blockchain
package test

// format: off

import io.iohk.decco._
import io.iohk.decco.auto._
import io.iohk.decco.test.utils.CodecTestingHelpers._
import io.iohk.multicrypto._
import io.iohk.multicrypto.encoding.implicits._
import io.iohk.multicrypto.test.utils.CryptoEntityArbitraries
import atala.obft.blockchain.models._
import atala.obft.blockchain.storage.{H2BlockStorage, InMemoryBlockStorage}
import atala.clock._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{BeforeAndAfter, MustMatchers, WordSpec}
import org.scalatest.OptionValues._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class BlockchainSpec extends WordSpec with MustMatchers with BeforeAndAfter with CryptoEntityArbitraries {

  implicit def genesisBlockArbitrary[T: Arbitrary]: Arbitrary[GenesisBlock[T]] =
    Arbitrary(
      for {
        ks <- arbitrary[List[SigningPublicKey]]
      } yield GenesisBlock[T](ks)
    )

  implicit val timeSlotArbitrary: Arbitrary[TimeSlot] =
    Arbitrary(arbitrary[Int].map(TimeSlot.apply))

  implicit val heightArbitrary: Arbitrary[Height] =
    Arbitrary(arbitrary[Int].suchThat(_ >= 0) map { i => Height.from(i).value })

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
        h <- arbitrary[Height]
        b <- arbitrary[BlockBody[T]]
        s <- arbitrary[Signature]
      } yield Block(h, b, s)
    )

  implicit def anyBlockArbitrary[T: Arbitrary]: Arbitrary[AnyBlock[T]] =
    Arbitrary(Gen.oneOf(arbitrary[Block[T]], arbitrary[GenesisBlock[T]]))

    def block(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
      val keyPair = keys((ts - 1) % keys.length)
      val key = keyPair.`private`
      val body = BlockBody(hash(previousBlock), List(s"A$ts", s"B$ts"), TimeSlot(ts), sign(TimeSlot(ts), key))
      Block(previousBlock.height.above, body, sign(body, key))
    }

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
      block.height mustBe blockchain.height.above
      block.body.delta mustBe List("A", "B")
      block.body.previousHash mustBe blockchain.genesisBlockHash
      block.body.timeSlot mustBe TimeSlot(3)
      block.body.timeSlotSignature mustBe sign(TimeSlot(3), keyPair1.`private`)
      block.signature mustBe sign(BlockBody(blockchain.genesisBlockHash, List("A", "B"), TimeSlot(3), sign(TimeSlot(3), keyPair1.`private`)), keyPair1.`private`)
    }

  }

  "Blockchain.add" should {

    "do nothing when the segment is empty" in {
      // GIVEN
      val storage = new InMemoryBlockStorage[String]
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), storage)(publicKeys, 1)
      val oldHeight = blockchain.height

      //WHEN
      blockchain.add(Nil)

      //THEN
      storage.data.isEmpty mustBe true
      blockchain.height mustBe oldHeight
      blockchain.headPointer mustBe blockchain.BlockPointer(blockchain.genesisBlockHash, Height.Zero)
    }

    "add a block, when the segment contains just that block (and it's valid)" in {
      // GIVEN
      val storage = new InMemoryBlockStorage[String]
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), storage)(publicKeys, 1)
      val oldHeight = blockchain.height

      val body = BlockBody(blockchain.genesisBlockHash, List("A", "B"), TimeSlot(3), sign(TimeSlot(3), keyPair1.`private`))
      val block = Block(blockchain.height, body, sign(body, keyPair1.`private`))

      //WHEN
      blockchain.add(block :: Nil)

      //THEN
      storage.data mustBe Map(hash(block) -> block)
      blockchain.headPointer mustBe blockchain.BlockPointer(hash(block: AnyBlock[String]), Height.from(1).value)
      blockchain.height mustBe oldHeight.above
    }


    "add all the blocks of the segment, when the whole segment is valid" in {
      // GIVEN
      val storage = new InMemoryBlockStorage[String]
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), storage)(publicKeys, 1)
      val oldHeight = blockchain.height

      val b1 = block(3, blockchain.genesisBlock)
      val b2 = block(4, b1)
      val b3 = block(7, b2)
      val segment = b3 :: b2 :: b1 :: Nil

      //WHEN
      blockchain.add(segment)

      //THEN
      val targetBlockchain = segment
      val targetStorage = targetBlockchain.map(b => hash(b) -> b).toMap
      storage.data.size mustBe targetStorage.size
      storage.data mustBe targetStorage
      blockchain.headPointer mustBe blockchain.BlockPointer(hash(b3), Height.from(3).value)
      blockchain.height.toInt mustBe 3
    }

    "change nothing, when the segment contains just one block (and it's invalid)" in {
      // GIVEN
      val storage = new InMemoryBlockStorage[String]
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), storage)(publicKeys, 1)
      val oldHeight = blockchain.height

      val body = BlockBody(blockchain.genesisBlockHash, List("A", "B"), TimeSlot(3), sign(TimeSlot(5), keyPair1.`private`))
      val block = Block(blockchain.height.above, body, sign(body, keyPair1.`private`))

      //WHEN
      blockchain.add(block :: Nil)

      //THEN
      storage.data.isEmpty mustBe true
      blockchain.headPointer mustBe blockchain.BlockPointer(blockchain.genesisBlockHash, Height.from(0).value)
      blockchain.height mustBe oldHeight
    }
   }

  "Blockchain.*run*" should {

    "use all transactions when invoking unsafeRunAllTransactions" in {
      // GIVEN
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), new InMemoryBlockStorage)(publicKeys, 1)

      val b1 = block(3, blockchain.genesisBlock)
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

      val b1 = block(3, blockchain.genesisBlock)
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

      val b1 = block(3, blockchain.genesisBlock)
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

      val b1 = block(3, blockchain.genesisBlock)
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

  "The Blockchain component" should {

    "load the genesis block on startup when InMemoryBlockStorage is empty" in {
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), new InMemoryBlockStorage[String])(publicKeys, 1)
      blockchain.headPointer.forceGetPointedBlockFromStorage mustBe genesisBlock
    }

    "load the highest block from storage on startup from inMemoryBlockStorage" in {
      val inMemoryBlockStorage = new InMemoryBlockStorage[String]
      val b1 = block(3, genesisBlock)
      val b2 = block(4, b1)
      val b3 = block(7, b2)
      inMemoryBlockStorage.put(hash(b1), b1)
      inMemoryBlockStorage.put(hash(b2), b2)
      inMemoryBlockStorage.put(hash(b3), b3)

      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), inMemoryBlockStorage)(publicKeys, 1)
      blockchain.headPointer.forceGetPointedBlockFromStorage mustBe b3
    }

    "load the genesis block on startup when H2BlockStorage is empty" in {
      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), h2BlockStorage)(publicKeys, 1)
      blockchain.headPointer.forceGetPointedBlockFromStorage mustBe genesisBlock
    }

    "load the highest block from storage on startup from H2BlockStorage" in {
      h2BlockStorage.put(hash(h2b1), h2b1)
      h2BlockStorage.put(hash(h2b2), h2b2)
      h2BlockStorage.put(hash(h2b3), h2b3)

      val blockchain = new Blockchain[String](new SegmentValidator(publicKeys), h2BlockStorage)(publicKeys, 1)
      blockchain.headPointer.forceGetPointedBlockFromStorage mustBe h2b3
    }
  }

  private val keyPair1 = generateSigningKeyPair()
  private val keyPair2 = generateSigningKeyPair()
  private val keys = List(keyPair1, keyPair2)
  private val publicKeys = keys.map(_.public)

  val h2BlockStorage = H2BlockStorage[String]("h2db")
  val genesisBlock: AnyBlock[String] = GenesisBlock(publicKeys)
  val h2b1 = block(3, genesisBlock)
  val h2b2 = block(4, h2b1)
  val h2b3 = block(7, h2b2)

  before {
    h2BlockStorage.remove(hash(h2b1))
    h2BlockStorage.remove(hash(h2b2))
    h2BlockStorage.remove(hash(h2b3))
  }

}

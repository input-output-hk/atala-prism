package atala.network
package test

import atala.clock.TimeSlot
import atala.helpers.monixhelpers._
import atala.obft.NetworkMessage
import atala.obft.blockchain.models._
import atala.config.ServerAddress
import io.iohk.decco.Codec
import io.iohk.decco.auto._
import io.iohk.decco.auto.instances.CollectionInstances.ListInstance
import io.iohk.decco.test.utils.CodecTestingHelpers._
import io.iohk.multicrypto._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.OptionValues._
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures._
import io.iohk.multicrypto.test.utils.CryptoEntityArbitraries
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global

class OBFTPeerGroupNetworkInterfaceSpec extends WordSpec with MustMatchers with CryptoEntityArbitraries {

  implicit class TaskOps[T](task: Task[T]) {

    def evaluated(implicit scheduler: Scheduler, patienceConfig: PatienceConfig): T = {
      task.runAsync(scheduler).futureValue
    }
  }

  implicit val timeSlotArbitrary: Arbitrary[TimeSlot] =
    Arbitrary(Gen.posNum[Int] map { x =>
      TimeSlot.from(x - 1).value
    })

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

  implicit def blockListArbitrary[T: Codec: Arbitrary]: Arbitrary[List[Block[T]]] =
    Arbitrary {
      Gen.listOf[Block[T]](arbitrary[Block[T]])
    }

  implicit def chainSegmentArbitrary[T: Codec: Arbitrary]: Arbitrary[ChainSegment[T]] =
    Arbitrary(
      for {
        list <- arbitrary[List[Block[T]]]
      } yield ChainSegment(list)
    )

  implicit def addTxArbitrary[T: Codec: Arbitrary]: Arbitrary[NetworkMessage.AddTransaction[T]] =
    Arbitrary(
      for {
        tx <- arbitrary[T]
      } yield NetworkMessage.AddTransaction(tx)
    )

  implicit def addBlockSegArbitrary[T: Codec: Arbitrary]: Arbitrary[NetworkMessage.AddBlockchainSegment[T]] =
    Arbitrary(
      for {
        seg <- arbitrary[ChainSegment[T]]
      } yield NetworkMessage.AddBlockchainSegment(seg)
    )

  implicit def networkMessageArbitrary[T: Codec: Arbitrary]: Arbitrary[NetworkMessage[T]] =
    Arbitrary(Gen.oneOf(arbitrary[NetworkMessage.AddTransaction[T]], arbitrary[NetworkMessage.AddBlockchainSegment[T]]))

  private val keyPair1 = generateSigningKeyPair()
  private val keyPair2 = generateSigningKeyPair()
  private val keys = List(keyPair1, keyPair2)
  private val publicKeys = keys.map(_.public)
  val genesisBlock: AnyBlock[String] = GenesisBlock(publicKeys)
  def createBlock(ts: Int, previousBlock: AnyBlock[String]): Block[String] = {
    val keyPair = keys((ts - 1) % keys.length)
    val key = keyPair.`private`
    val body = BlockBody(
      hash(previousBlock),
      List(s"A$ts", s"B$ts"),
      TimeSlot.from(ts).value,
      sign(TimeSlot.from(ts).value, key)
    )
    Block(body, sign(body, key))
  }
  private val block = createBlock(3, genesisBlock)

  "The encoders and crypto functions" should {
    "work with OBFT entities" in {
      testCodec[String]
      testCodec[Block[String]]
      testCodec[List[Block[String]]]
      testCodec[ChainSegment[String]]
      testCodec[NetworkMessage.AddTransaction[String]]
      testCodec[NetworkMessage.AddBlockchainSegment[String]]
      testCodec[NetworkMessage[String]]
    }
  }

  "OBFTPeerGroupNetworkFactory" should {
    "create interfaces between two nodes" in twoUDPnetworkInterfaces[String] { (aliceChannel, bobChannel) =>
      val bobRecived = bobChannel.in.headL.runAsync
      val msg = NetworkMessage.AddBlockchainSegment(ChainSegment.empty[String])

      aliceChannel.out.feedItem(msg)

      bobRecived.futureValue mustBe msg
    }
  }

  def twoUDPnetworkInterfaces[Tx: Codec](
      testCode: (OBFTNetworkInterface[Tx], OBFTNetworkInterface[Tx]) => Any
  ): Unit = {
    val aliceAddress = ServerAddress("localhost", 8001)
    val bobAddress = ServerAddress("localhost", 8002)
    val alice = OBFTNetworkFactory[Tx](1, aliceAddress, Set(2 -> bobAddress))
    val bob = OBFTNetworkFactory[Tx](2, bobAddress, Set(1 -> aliceAddress))
    val aliceChannel = alice.initialise().evaluated
    val bobChannel = bob.initialise().evaluated
    try {
      testCode(aliceChannel, bobChannel)
    } finally {
      aliceChannel.shutdown()
      bobChannel.shutdown()
    }
  }
}

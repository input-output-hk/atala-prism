package obft
package test

import monix.execution.Scheduler.Implicits.global
import monix.reactive.MulticastStrategy
import monix.reactive.subjects.ConcurrentSubject
import obft.blockchain._
import obft.clock.TimeSlot
import io.iohk.multicrypto._
import obft.mempool.MemPool
import org.scalatest.EitherValues._
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.duration.DurationInt

class OuroborosBFTSpec extends WordSpec {

  import OuroborosBFTSpec._

  implicit val patienceConfig = PatienceConfig(timeout = 10.seconds, interval = 1.second)

  val clusterSize = 5
  val myId = 3
  val clusterKeyPairs = (1 to clusterSize).map(_ => generateSigningKeyPair()).toList
  val genesisKeys = clusterKeyPairs.map(_.public)
  val myKeyPair = clusterKeyPairs(myId)

  "getting a clock signal" should {
    "produce a Tick" in {
      val data = create(myId, myKeyPair, genesisKeys)
      val tick = Tick(TimeSlot(0))
      data.inputStreamClockSignals.feedItem(tick)

      whenReady(data.obft.ouroborosStream.headL.runAsync) { result =>
        result.right.value must be(tick)
      }
    }

    "advance the mempool" in {
      val data = create(myId, myKeyPair, genesisKeys)
      val old = data.mempool.level
      val tick = Tick(TimeSlot(1))
      data.inputStreamClockSignals.feedItem(tick)
      whenReady(data.obft.ouroborosStream.headL.runAsync)(_ => ())

      val last = data.mempool.level
      last must be(old + 1)
    }

    "not extend the blockchain if server is the leader but the mempool is empty" in {
      val data = create(myId, myKeyPair, genesisKeys)
      val old = data.blockchain.level
      val tick = Tick(TimeSlot(myId))
      data.inputStreamClockSignals.feedItem(tick)

      val last = data.blockchain.level
      last must be(old)
    }

    "extend the blockchain and replicate the segment if the server is the leader" in {
      val data = create(myId, myKeyPair, genesisKeys)
      val old = data.blockchain.level
      val tick = Tick(TimeSlot(myId))
      data.inputStreamClockSignals.feedItem(tick)

      data.mempool.add("One")
      whenReady(data.obft.ouroborosStream.headL.runAsync) { result =>
        result.right.value must be(tick)
      }

      whenReady(data.outputStreamDiffuseToRestOfCluster.headL.runAsync) { result =>
        result.isInstanceOf[Message.AddBlockchainSegment[Tx]] must be(true)
      }
      val last = data.blockchain.level
      last must be(old + 1)
    }
  }

  "getting the AddTransaction message" should {
    "add the transaction to the mempool" in {
      val data = create(myId, myKeyPair, genesisKeys)
      val transaction = "example"
      data.inputStreamMessages.feedItem(Message.AddTransaction(transaction))
      whenReady(data.obft.ouroborosStream.headL.runAsync) { result =>
        result.left.value must be(Message.AddTransaction(transaction))
      }

      data.mempool.collect() must be(List(transaction))
    }
  }

  "getting the AddBlockchainSegment message" should {
    "add the segment to the blockchain " in {
      val data = create(myId, myKeyPair, genesisKeys)
      val transaction = "example"
      val tick = Tick(TimeSlot(myId))

      val blockData = data.blockchain.createBlockData(List(transaction), tick.timeSlot, myKeyPair.`private`)
      val segment = List(blockData)
      val old = data.blockchain.level

      data.inputStreamMessages.feedItem(Message.AddBlockchainSegment(segment))
      whenReady(data.obft.ouroborosStream.headL.runAsync) { result =>
        result.left.value must be(Message.AddBlockchainSegment(segment))
      }

      val last = data.blockchain.level
      last must be(old + 1)
    }
  }
}

object OuroborosBFTSpec {

  import io.iohk.decco.auto._

  type Tx = String

  def multicastStrategy[A] = MulticastStrategy.replay[A]

  val transactionTTL = 2
  val maxNumOfAdversaries = 0

  class FakeBlockchain(genesisKeys: List[SigningPublicKey])
      extends Blockchain[Tx](new SegmentValidator(genesisKeys), new BlockStorage)(genesisKeys, maxNumOfAdversaries) {

    var level = 0

    override def add(chainSegment: List[Block[Tx]]): Unit = {
      level = level + 1
      super.add(chainSegment)
    }
  }

  class FakeMemPool extends MemPool[Tx](transactionTTL) {

    var level = 0

    override def advance(): Unit = {
      level = level + 1
      super.advance()
    }
  }

  case class Data(
      obft: OuroborosBFT[Tx],
      blockchain: FakeBlockchain,
      mempool: FakeMemPool,
      inputStreamClockSignals: ConcurrentSubject[Tick, Tick],
      inputStreamMessages: ConcurrentSubject[Message[Tx], Message[Tx]],
      outputStreamDiffuseToRestOfCluster: ConcurrentSubject[Message.AddBlockchainSegment[Tx], Message.AddBlockchainSegment[
        Tx
      ]]
  )

  def create(myId: Int, myKeyPair: SigningKeyPair, genesisKeys: List[SigningPublicKey]): Data = {

    val inputStreamClockSignals = ConcurrentSubject[Tick](multicastStrategy)
    val inputStreamMessages = ConcurrentSubject[Message[Tx]](multicastStrategy)
    val outputStreamDiffuseToRestOfCluster = ConcurrentSubject[Message.AddBlockchainSegment[Tx]](multicastStrategy)

    val blockchain = new FakeBlockchain(genesisKeys)
    val mempool = new FakeMemPool
    val obft: OuroborosBFT[Tx] = new OuroborosBFT(blockchain, mempool)(
      i = myId,
      keyPair = myKeyPair,
      clusterSize = genesisKeys.size,
      inputStreamClockSignals = inputStreamClockSignals,
      inputStreamMessages = inputStreamMessages,
      outputStreamDiffuseToRestOfCluster = outputStreamDiffuseToRestOfCluster
    )

    Data(
      obft = obft,
      blockchain = blockchain,
      mempool = mempool,
      inputStreamClockSignals = inputStreamClockSignals,
      inputStreamMessages = inputStreamMessages,
      outputStreamDiffuseToRestOfCluster = outputStreamDiffuseToRestOfCluster
    )
  }
}

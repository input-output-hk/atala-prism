package io.iohk.cef.integration

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{MessageEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.core.{Envelope, NodeCore}
import io.iohk.cef.frontend.client.ChimericServiceApi
import io.iohk.cef.frontend.models.ChimericTransactionRequest
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, ByteStringSerializable, Transaction}
import io.iohk.cef.network.{MessageStream, Network, NodeId}
import io.iohk.cef.transactionpool.{TimedQueue, TransactionPoolActorModelInterface, TransactionPoolFutureInterface}
import io.iohk.cef.utils.ByteSizeable
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{ExecutionContext, Future}

class ChimericTransactionNodeCoreItSpec
    extends FlatSpec
    with MustMatchers
    with BeforeAndAfterAll
    with ScalaFutures
    with MockitoSugar
    with ScalatestRouteTest {

  import ChimericTransactionNodeCoreItSpec._

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  behavior of "ChimericTransactionNodeCoreItSpec"

  def createNodeCore: NodeCore[ChimericStateResult, ChimericBlockHeader, ChimericTx] = {
    implicit val timeout = Timeout(10.seconds)
    implicit val envelopeSerializable = mock[ByteStringSerializable[Envelope[TransactionType]]]
    implicit val blockSerializable = mock[ByteStringSerializable[Envelope[BlockType]]]

    def generateHeader(transactions: Seq[TransactionType]) = {
      new ChimericBlockHeader
    }

    val ledgerStateStorage = mock[LedgerStateStorageType]
    val queue = new TimedQueueType()
    val txPoolActorModelInterface =
      new TestableTransactionPoolActorModelInterface(generateHeader, 10000, ledgerStateStorage, 10.minutes, queue)

    val txPoolFutureInterface = new TransactionPoolFutureInterfaceType(txPoolActorModelInterface)

    val consensus = mock[ConsensusType]
    val blockNetwork = mock[Network[Envelope[BlockType]]]

    val mockTxMessageStream = mock[MessageStream[Envelope[TransactionType]]]

    val mockBlockMessageStream = mock[MessageStream[Envelope[BlockType]]]

    val txNetwork = mock[Network[Envelope[TransactionType]]]

    when(ledgerStateStorage.slice(ArgumentMatchers.any())).thenReturn(new ChimericLedgerState(Map.empty))
    when(txNetwork.messageStream).thenReturn(mockTxMessageStream)
    when(blockNetwork.messageStream).thenReturn(mockBlockMessageStream)
    when(mockTxMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    when(mockBlockMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))

    val consensusMap = Map(1 -> (txPoolFutureInterface, consensus))

    val me = NodeId("3112")

    val core =
      new NodeCore(consensusMap, txNetwork, blockNetwork, me)(
        envelopeSerializable,
        blockSerializable,
        ExecutionContext.global)

    core.asInstanceOf[NodeCore[ChimericStateResult, ChimericBlockHeader, ChimericTx]]
  }

  it should "process a transaction" in {
    val testTransaction = ChimericTx(List(CreateCurrency("BTC")))

    val node = createNodeCore
    val service = new ChimericTransactionService(node)
    val api = new ChimericServiceApi(service)
    val routes = api.create

    val entity = ChimericTransactionRequest(testTransaction, 1)
    val json = Marshal(entity).to[MessageEntity].futureValue

    val request = Post("/chimeric-transactions", json)

    request ~> routes ~> check {
      status must ===(StatusCodes.Created)
    }
  }
}

object ChimericTransactionNodeCoreItSpec {

  type TransactionStateType = ChimericStateResult
  type BlockHeaderType = ChimericBlockHeader
  type TransactionType = Transaction[TransactionStateType]
  type BlockType = Block[TransactionStateType, BlockHeaderType, TransactionType]

  type TransactionPoolFutureInterfaceType =
    TransactionPoolFutureInterface[TransactionStateType, BlockHeaderType, TransactionType]

  type LedgerStateStorageType = LedgerStateStorage[TransactionStateType]
  type TimedQueueType = TimedQueue[TransactionType]

  type ByteSizeableType = ByteSizeable[BlockType]
  type TransactionPoolActorModelInterfaceType =
    TransactionPoolActorModelInterface[TransactionStateType, BlockHeaderType, Transaction[TransactionStateType]]

  type ConsensusType = Consensus[TransactionStateType, BlockHeaderType, TransactionType]

  implicit val blockSizeable = new ByteSizeable[BlockType] {
    override def sizeInBytes(t: BlockType): Int = 1
  }

  class TestableTransactionPoolActorModelInterface(
      headerGenerator: Seq[TransactionType] => BlockHeaderType,
      maxTxSizeInBytes: Int,
      ledgerStateStorage: LedgerStateStorageType,
      defaultDurationTxs: Duration,
      timedQueue: TimedQueueType)(implicit system: ActorSystem)
      extends TransactionPoolActorModelInterfaceType(
        system.actorOf,
        headerGenerator,
        maxTxSizeInBytes,
        ledgerStateStorage,
        defaultDurationTxs,
        () => timedQueue) {

    lazy val testActorRef = TestActorRef[TransactionPoolActor](Props(new TransactionPoolActor()))

    override def poolActor: ActorRef = testActorRef
  }
}

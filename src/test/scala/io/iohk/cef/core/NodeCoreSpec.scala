package io.iohk.cef.core
import akka.util.{ByteString, Timeout}
import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.network.{NetworkComponent, NetworkError, NodeId}
import io.iohk.cef.test.{DummyBlockHeader, DummyTransaction}
import io.iohk.cef.transactionpool.TransactionPoolFutureInterface
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, MustMatchers}

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._

class NodeCoreSpec extends AsyncFlatSpec with MustMatchers with MockitoSugar {

  type State = String
  type Header = DummyBlockHeader
  type Tx = DummyTransaction
  type BlockType = Block[State, Header, Tx]

  def mockTxPoolFutureInterface: TransactionPoolFutureInterface[State, Header, Tx] =
    mock[TransactionPoolFutureInterface[State, Header, Tx]]

  def mockConsensus: Consensus[State, Tx] = mock[Consensus[State, Tx]]

  def mockNetworkComponent: NetworkComponent[State] = mock[NetworkComponent[State]]

  def mockByteStringSerializable: ByteStringSerializable[Tx] =
    mock[ByteStringSerializable[Tx]]

  def mockBlockSerializable: ByteStringSerializable[BlockType] =
    mock[ByteStringSerializable[BlockType]]

  val timeout = Timeout(1 minute)

  def setupTest(ledgerId: LedgerId, me: NodeId = NodeId(ByteString(1)))(
      implicit txSerializable: ByteStringSerializable[Tx],
      blockSerializable: ByteStringSerializable[Block[State, Header, Tx]]): NodeCore[State, Header, Tx] = {
    val consensusMap = Map(ledgerId -> (mockTxPoolFutureInterface, mockConsensus))
    val networkComponent = mockNetworkComponent
    implicit val t = timeout
    new NodeCore(consensusMap, networkComponent, me)
  }

  behavior of "NodeSpec"

  it should "receive a transaction" in {
    val testTx = DummyTransaction(10)
    val ledgerId = 1
    val testEnvelope = Envelope(testTx, 1, _ => true)
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val core = setupTest(ledgerId)
    when(core.networkComponent.disseminate(testEnvelope))
      .thenReturn(Future.successful(Right(())))
    when(core.consensusMap(ledgerId)._1.processTransaction(testEnvelope.content))
      .thenReturn(Future.successful(Right(())))
    core.receiveTransaction(testEnvelope).map(r =>{
      verify(core.networkComponent, times(1)).disseminate(testEnvelope)
      verify(core.consensusMap(ledgerId)._1, times(1)).processTransaction(testEnvelope.content)
      r mustBe Right(())
    })
  }

  it should "receive a block" in {
    val testBlock = Block(DummyBlockHeader(1), immutable.Seq(DummyTransaction(10)))
    val ledgerId = 1
    val testEnvelope = Envelope(testBlock, 1, _ => true)
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val core = setupTest(ledgerId)
    when(core.networkComponent.disseminate(testEnvelope))
      .thenReturn(Future.successful(Right(())))
    when(core.consensusMap(ledgerId)._2.process(testEnvelope.content))
      .thenReturn(Future.successful(Right(())))
    core.receiveBlock(testEnvelope).map(r =>{
      verify(core.networkComponent, times(1)).disseminate(testEnvelope)
      verify(core.consensusMap(ledgerId)._2, times(1)).process(testEnvelope.content)
      r mustBe Right(())
    })
  }

  it should "avoid processing a block if this is not a receiver" in {
    val (testBlockEnvelope, _, core, _, ledgerId, _, bs, error) = setupMissingCapabilitiesTest(_ => false)
    implicit val serializable = bs
    when(core.networkComponent.disseminate(testBlockEnvelope))
      .thenReturn(Future.successful(error))
    for {
      rcv <- core.receiveBlock(testBlockEnvelope)
    } yield {
      verify(core.networkComponent, times(1)).disseminate(testBlockEnvelope)
      verify(core.consensusMap(ledgerId)._2, times(0)).process(testBlockEnvelope.content)
      rcv mustBe error
    }
  }

  it should "avoid processing a tx if this is not a receiver" in {
    val (_, testTxEnvelope, core, _, ledgerId, bs, _, error) = setupMissingCapabilitiesTest(_ => false)
    implicit val serializable = bs
    when(core.networkComponent.disseminate(testTxEnvelope))
      .thenReturn(Future.successful(error))
    for {
      rcv <- core.receiveTransaction(testTxEnvelope)
    } yield {
      verify(core.networkComponent, times(1)).disseminate(testTxEnvelope)
      verify(core.consensusMap(ledgerId)._1, times(0)).processTransaction(testTxEnvelope.content)
      rcv mustBe error
    }
  }

  it should "avoid processing a block if the node doesn't participate in consensus" in {
    val (testBlockTxEnvelope, _, core, me, ledgerId, _, bs, _) = setupMissingCapabilitiesTest(_ => true)
    implicit val serializable = bs
    val newEnvelope = testBlockTxEnvelope.copy(ledgerId = ledgerId + 1)
    when(core.networkComponent.disseminate(newEnvelope))
      .thenReturn(Future.successful(Right(())))
    for {
      rcv <- core.receiveBlock(newEnvelope)
    } yield {
      verify(core.networkComponent, times(1)).disseminate(newEnvelope)
      verify(core.consensusMap(ledgerId)._2, times(0)).process(newEnvelope.content)
      rcv mustBe Left(MissingCapabilitiesForTx(me, newEnvelope))
    }
  }

  it should "avoid processing a tx if the node doesn't participate in consensus" in {
    val (_, testTxEnvelope, core, me, ledgerId, bs, _, _) = setupMissingCapabilitiesTest(_ => true)
    implicit val serializable = bs
    val newEnvelope = testTxEnvelope.copy(ledgerId = ledgerId + 1)
    when(core.networkComponent.disseminate(newEnvelope))
      .thenReturn(Future.successful(Right(())))
    for {
      rcv <- core.receiveTransaction(newEnvelope)
    } yield {
      verify(core.networkComponent, times(1)).disseminate(newEnvelope)
      verify(core.consensusMap(ledgerId)._1, times(0)).processTransaction(newEnvelope.content)
      rcv mustBe Left(MissingCapabilitiesForTx(me, newEnvelope))
    }
  }

  private def setupMissingCapabilitiesTest(destinationDescriptor: DestinationDescriptor) = {
    val testTx = DummyTransaction(10)
    val testBlock = Block(DummyBlockHeader(1), immutable.Seq(testTx))
    val ledgerId = 1
    val testBlockEnvelope = Envelope(testBlock, 1, destinationDescriptor)
    val testTxEnvelope = Envelope(testTx, 1, destinationDescriptor)
    val me = NodeId(ByteString("Me"))
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val core = setupTest(ledgerId, me)
    val error = Left(new NetworkError {})
    (testBlockEnvelope, testTxEnvelope, core, me, ledgerId, bs1, bs2, error)
  }
}

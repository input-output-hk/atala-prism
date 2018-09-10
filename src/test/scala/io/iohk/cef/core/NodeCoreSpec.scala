package io.iohk.cef.core
import akka.util.{ByteString, Timeout}
import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.network.{NetworkError, NodeId}
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

  private def setupTest(ledgerId: LedgerId, me: NodeId = NodeId(ByteString(1)))(
      implicit txSerializable: ByteStringSerializable[Tx],
      blockSerializable: ByteStringSerializable[Block[State, Header, Tx]]) = {
    val consensusMap = Map(ledgerId -> (mockTxPoolFutureInterface, mockConsensus))
    val networkComponent = mockNetworkComponent
    implicit val t = timeout
    (new NodeCore(consensusMap, networkComponent, me), consensusMap, networkComponent)
  }

  behavior of "NodeSpec"

  it should "receive a transaction" in {
    val testTx = DummyTransaction(10)
    val ledgerId = 1
    val testEnvelope = Envelope(testTx, 1, _ => true)
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val (core, consensusMap, networkComponent) = setupTest(ledgerId)
    when(networkComponent.disseminate(testEnvelope))
      .thenReturn(Future.successful(Right(())))
    when(consensusMap(ledgerId)._1.processTransaction(testEnvelope.content))
      .thenReturn(Future.successful(Right(())))
    core
      .receiveTransaction(testEnvelope)
      .map(r => {
        verify(networkComponent, times(1)).disseminate(testEnvelope)
        verify(consensusMap(ledgerId)._1, times(1)).processTransaction(testEnvelope.content)
        r mustBe Right(())
      })
  }

  it should "receive a block" in {
    val testBlock = Block(DummyBlockHeader(1), immutable.Seq(DummyTransaction(10)))
    val ledgerId = 1
    val testEnvelope = Envelope(testBlock, 1, _ => true)
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val (core, consensusMap, networkComponent) = setupTest(ledgerId)
    when(networkComponent.disseminate(testEnvelope))
      .thenReturn(Future.successful(Right(())))
    when(consensusMap(ledgerId)._2.process(testEnvelope.content))
      .thenReturn(Future.successful(Right(())))
    core
      .receiveBlock(testEnvelope)
      .map(r => {
        verify(networkComponent, times(1)).disseminate(testEnvelope)
        verify(consensusMap(ledgerId)._2, times(1)).process(testEnvelope.content)
        r mustBe Right(())
      })
  }

  it should "avoid processing a block if this is not a receiver" in {
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = 1
    val me = NodeId(ByteString("Me"))
    val (core, consensusMap, networkComponent) = setupTest(ledgerId, me)
    val (testBlockEnvelope, _, error) = setupMissingCapabilitiesTest(ledgerId, core, _ => false, me)
    when(networkComponent.disseminate(testBlockEnvelope))
      .thenReturn(Future.successful(error))
    for {
      rcv <- core.receiveBlock(testBlockEnvelope)
    } yield {
      verify(networkComponent, times(1)).disseminate(testBlockEnvelope)
      verify(consensusMap(ledgerId)._2, times(0)).process(testBlockEnvelope.content)
      rcv mustBe error
    }
  }

  it should "avoid processing a tx if this is not a receiver" in {
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = 1
    val me = NodeId(ByteString("Me"))
    val (core, consensusMap, networkComponent) = setupTest(ledgerId, me)
    val (_, testTxEnvelope, error) = setupMissingCapabilitiesTest(ledgerId, core, _ => false, me)
    when(networkComponent.disseminate(testTxEnvelope))
      .thenReturn(Future.successful(error))
    for {
      rcv <- core.receiveTransaction(testTxEnvelope)
    } yield {
      verify(networkComponent, times(1)).disseminate(testTxEnvelope)
      verify(consensusMap(ledgerId)._1, times(0)).processTransaction(testTxEnvelope.content)
      rcv mustBe error
    }
  }

  it should "avoid processing a block if the node doesn't participate in consensus" in {
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = 1
    val me = NodeId(ByteString("Me"))
    val (core, consensusMap, networkComponent) = setupTest(ledgerId, me)
    val (testBlockTxEnvelope, _, _) = setupMissingCapabilitiesTest(ledgerId, core, _ => true, me)
    val newEnvelope = testBlockTxEnvelope.copy(ledgerId = ledgerId + 1)
    when(networkComponent.disseminate(newEnvelope))
      .thenReturn(Future.successful(Right(())))
    for {
      rcv <- core.receiveBlock(newEnvelope)
    } yield {
      verify(networkComponent, times(1)).disseminate(newEnvelope)
      verify(consensusMap(ledgerId)._2, times(0)).process(newEnvelope.content)
      rcv mustBe Left(MissingCapabilitiesForTx(me, newEnvelope))
    }
  }

  it should "avoid processing a tx if the node doesn't participate in consensus" in {
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = 1
    val me = NodeId(ByteString("Me"))
    val (core, consensusMap, networkComponent) = setupTest(ledgerId, me)
    val (_, testTxEnvelope, _) = setupMissingCapabilitiesTest(ledgerId, core, _ => true, me)
    val newEnvelope = testTxEnvelope.copy(ledgerId = ledgerId + 1)
    when(networkComponent.disseminate(newEnvelope))
      .thenReturn(Future.successful(Right(())))
    for {
      rcv <- core.receiveTransaction(newEnvelope)
    } yield {
      verify(networkComponent, times(1)).disseminate(newEnvelope)
      verify(consensusMap(ledgerId)._1, times(0)).processTransaction(newEnvelope.content)
      rcv mustBe Left(MissingCapabilitiesForTx(me, newEnvelope))
    }
  }

  private def setupMissingCapabilitiesTest(
      ledgerId: LedgerId,
      core: NodeCore[String, DummyBlockHeader, DummyTransaction],
      destinationDescriptor: DestinationDescriptor,
      me: NodeId)(
      implicit txSerializable: ByteStringSerializable[Tx],
      blockSerializable: ByteStringSerializable[Block[State, Header, Tx]]) = {
    val testTx = DummyTransaction(10)
    val testBlock = Block(DummyBlockHeader(1), immutable.Seq(testTx))
    val ledgerId = 1
    val testBlockEnvelope = Envelope(testBlock, 1, destinationDescriptor)
    val testTxEnvelope = Envelope(testTx, 1, destinationDescriptor)
    val core = setupTest(ledgerId, me)
    val error = Left(new NetworkError {})
    (testBlockEnvelope, testTxEnvelope, error)
  }
}

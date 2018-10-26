package io.iohk.cef.core
import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.network.{MessageStream, Network, NodeId}
import io.iohk.cef.test.{DummyBlockHeader, DummyTransaction}
import io.iohk.cef.transactionpool.TransactionPoolInterface
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, MustMatchers}

import scala.collection.immutable
import scala.concurrent.Future

class NodeCoreSpec extends AsyncFlatSpec with MustMatchers with MockitoSugar {

  def mockConsensus: Consensus[String, DummyBlockHeader, DummyTransaction] =
    mock[Consensus[String, DummyBlockHeader, DummyTransaction]]

  def mockTxPoolFutureInterface: TransactionPoolInterface[String, DummyBlockHeader, DummyTransaction] =
    mock[TransactionPoolInterface[String, DummyBlockHeader, DummyTransaction]]

  type State = String
  type Header = DummyBlockHeader
  type Tx = DummyTransaction
  type BlockType = Block[State, Header, Tx]

  def mockNetwork[M]: Network[M] = mock[Network[M]]

  def mockByteStringSerializable: ByteStringSerializable[Envelope[Tx]] =
    mock[ByteStringSerializable[Envelope[Tx]]]

  def mockBlockSerializable: ByteStringSerializable[Envelope[BlockType]] =
    mock[ByteStringSerializable[Envelope[BlockType]]]

  private def setupTest(ledgerId: LedgerId, me: NodeId = NodeId("abcd"))(
      implicit txSerializable: ByteStringSerializable[Envelope[Tx]],
      blockSerializable: ByteStringSerializable[Envelope[Block[State, Header, Tx]]]) = {
    val consensusMap = Map(ledgerId -> (mockTxPoolFutureInterface, mockConsensus))
    val txDM = mockNetwork[Envelope[DummyTransaction]]
    val blockDM = mockNetwork[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]
    val txMessageStream = mock[MessageStream[Envelope[DummyTransaction]]]
    val blockMessageStream = mock[MessageStream[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]]
    when(txDM.messageStream).thenReturn(txMessageStream)
    when(blockDM.messageStream).thenReturn(blockMessageStream)
    when(txMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    when(blockMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    (
      new NodeCore(
        consensusMap,
        txDM,
        blockDM,
        me
      ),
      consensusMap,
      txDM,
      blockDM)
  }

  behavior of "NodeSpec"

  it should "receive a transaction" in {
    val testTx = DummyTransaction(10)
    val ledgerId = "1"
    val testEnvelope = Envelope(testTx, ledgerId, Everyone)
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val (core, consensusMap, txDM, _) = setupTest(ledgerId)
    when(consensusMap(ledgerId)._1.processTransaction(testEnvelope.content))
      .thenReturn(Right(()))
    core
      .receiveTransaction(testEnvelope)
      .map(r => {
        verify(txDM, times(1)).disseminateMessage(testEnvelope)
        verify(consensusMap(ledgerId)._1, times(1)).processTransaction(testEnvelope.content)
        r mustBe Right(())
      })
  }

  it should "receive a block" in {
    val testBlock = Block(DummyBlockHeader(1), immutable.Seq(DummyTransaction(10)))
    val ledgerId = "1"
    val testEnvelope = Envelope(testBlock, ledgerId, Everyone)
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val (core, consensusMap, _, blockDM) = setupTest(ledgerId)
    when(consensusMap(ledgerId)._2.process(testEnvelope.content))
      .thenReturn(Future.successful(Right(())))
    core
      .receiveBlock(testEnvelope)
      .map(r => {
        verify(blockDM, times(1)).disseminateMessage(testEnvelope)
        verify(consensusMap(ledgerId)._2, times(1)).process(testEnvelope.content)
        r mustBe Right(())
      })
  }

  it should "avoid processing a block if this is not a receiver" in {
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = "1"
    val me = NodeId("abcd")
    val (core, consensusMap, _, blockDM) = setupTest(ledgerId, me)
    val (testBlockEnvelope, _) = setupMissingCapabilitiesTest(ledgerId, core, Not(Everyone), me)
    for {
      rcv <- core.receiveBlock(testBlockEnvelope)
    } yield {
      verify(blockDM, times(1)).disseminateMessage(testBlockEnvelope)
      verify(consensusMap(ledgerId)._2, times(0)).process(testBlockEnvelope.content)
      rcv mustBe Right(())
    }
  }

  it should "avoid processing a tx if this is not a receiver" in {
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = "1"
    val me = NodeId("abcd")
    val (core, consensusMap, txDM, _) = setupTest(ledgerId, me)
    val (_, testTxEnvelope) = setupMissingCapabilitiesTest(ledgerId, core, Not(Everyone), me)
    for {
      rcv <- core.receiveTransaction(testTxEnvelope)
    } yield {
      verify(txDM, times(1)).disseminateMessage(testTxEnvelope)
      verify(consensusMap(ledgerId)._1, times(0)).processTransaction(testTxEnvelope.content)
      rcv mustBe Right(())
    }
  }

  it should "avoid processing a block if the node doesn't participate in consensus" in {
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = "1"
    val me = NodeId("abcd")
    val (core, consensusMap, _, blockDM) = setupTest(ledgerId, me)
    val (testBlockTxEnvelope, _) = setupMissingCapabilitiesTest(ledgerId, core, Everyone, me)
    val newEnvelope = testBlockTxEnvelope.copy(ledgerId = ledgerId + 1)
    for {
      rcv <- core.receiveBlock(newEnvelope)
    } yield {
      verify(blockDM, times(1)).disseminateMessage(newEnvelope)
      verify(consensusMap(ledgerId)._2, times(0)).process(newEnvelope.content)
      rcv mustBe Left(MissingCapabilitiesForTx(me, newEnvelope))
    }
  }

  it should "avoid processing a tx if the node doesn't participate in consensus" in {
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = "1"
    val me = NodeId("abcd")
    val (core, consensusMap, txDM, _) = setupTest(ledgerId, me)
    val (_, testTxEnvelope) = setupMissingCapabilitiesTest(ledgerId, core, Everyone, me)
    val newEnvelope = testTxEnvelope.copy(ledgerId = ledgerId + 1)
    for {
      rcv <- core.receiveTransaction(newEnvelope)
    } yield {
      verify(txDM, times(1)).disseminateMessage(newEnvelope)
      verify(consensusMap(ledgerId)._1, times(0)).processTransaction(newEnvelope.content)
      rcv mustBe Left(MissingCapabilitiesForTx(me, newEnvelope))
    }
  }

  private def setupMissingCapabilitiesTest(
      ledgerId: LedgerId,
      core: NodeCore[String, DummyBlockHeader, DummyTransaction],
      destinationDescriptor: DestinationDescriptor,
      me: NodeId)(
      implicit txSerializable: ByteStringSerializable[Envelope[Tx]],
      blockSerializable: ByteStringSerializable[Envelope[Block[State, Header, Tx]]]) = {
    val testTx = DummyTransaction(10)
    val testBlock = Block(DummyBlockHeader(1), immutable.Seq(testTx))
    val testBlockEnvelope = Envelope(testBlock, "1", destinationDescriptor)
    val testTxEnvelope = Envelope(testTx, "1", destinationDescriptor)
    (testBlockEnvelope, testTxEnvelope)
  }
}

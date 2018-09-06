package io.iohk.cef.core
import akka.util.{ByteString, Timeout}
import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.network.{DisseminationalNetwork, NetworkError, NodeId}
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

  def mockDisseminationalNetwork[M]: DisseminationalNetwork[M] = mock[DisseminationalNetwork[M]]

  def mockByteStringSerializable: ByteStringSerializable[Tx] =
    mock[ByteStringSerializable[Tx]]

  def mockBlockSerializable: ByteStringSerializable[BlockType] =
    mock[ByteStringSerializable[BlockType]]

  val timeout = Timeout(1 minute)

  private def setupTest(ledgerId: LedgerId, me: NodeId = NodeId(ByteString(1)))(
      implicit txSerializable: ByteStringSerializable[Tx],
      blockSerializable: ByteStringSerializable[Block[State, Header, Tx]]) = {
    val consensusMap = Map(ledgerId -> (mockTxPoolFutureInterface, mockConsensus))
    val txDM = mockDisseminationalNetwork[Envelope[DummyTransaction]]
    val blockDM = mockDisseminationalNetwork[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]
    implicit val t = timeout
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
    val ledgerId = 1
    val testEnvelope = Envelope(testTx, 1, _ => true)
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val (core, consensusMap, txDM, _) = setupTest(ledgerId)
    when(txDM.disseminateMessage(testEnvelope)).thenReturn(())
    when(consensusMap(ledgerId)._1.processTransaction(testEnvelope.content))
      .thenReturn(Future.successful(Right(())))
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
    val ledgerId = 1
    val testEnvelope = Envelope(testBlock, 1, _ => true)
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val (core, consensusMap, _, blockDM) = setupTest(ledgerId)
    when(blockDM.disseminateMessage(testEnvelope)).thenReturn(())
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
    pending //Waiting to see if a Disseminational network can return errors
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = 1
    val me = NodeId(ByteString("Me"))
    val (core, consensusMap, _, blockDM) = setupTest(ledgerId, me)
    val (testBlockEnvelope, _, error) = setupMissingCapabilitiesTest(ledgerId, core, _ => false, me)
//    when(blockDM.disseminateMessage(testBlockEnvelope))
//      .thenReturn(Future.successful(error))
    for {
      rcv <- core.receiveBlock(testBlockEnvelope)
    } yield {
      verify(blockDM, times(1)).disseminateMessage(testBlockEnvelope)
      verify(consensusMap(ledgerId)._2, times(0)).process(testBlockEnvelope.content)
      rcv mustBe error
    }
  }

  it should "avoid processing a tx if this is not a receiver" in {
    pending //Waiting to see if a Disseminational network can return errors
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = 1
    val me = NodeId(ByteString("Me"))
    val (core, consensusMap, txDM, _) = setupTest(ledgerId, me)
    val (_, testTxEnvelope, error) = setupMissingCapabilitiesTest(ledgerId, core, _ => false, me)
//    when(txDM.disseminateMessage(testTxEnvelope))
//      .thenReturn(Future.successful(error))
    for {
      rcv <- core.receiveTransaction(testTxEnvelope)
    } yield {
      verify(txDM, times(1)).disseminateMessage(testTxEnvelope)
      verify(consensusMap(ledgerId)._1, times(0)).processTransaction(testTxEnvelope.content)
      rcv mustBe error
    }
  }

  it should "avoid processing a block if the node doesn't participate in consensus" in {
    implicit val bs1 = mockByteStringSerializable
    implicit val bs2 = mockBlockSerializable
    val ledgerId = 1
    val me = NodeId(ByteString("Me"))
    val (core, consensusMap, _, blockDM) = setupTest(ledgerId, me)
    val (testBlockTxEnvelope, _, _) = setupMissingCapabilitiesTest(ledgerId, core, _ => true, me)
    val newEnvelope = testBlockTxEnvelope.copy(ledgerId = ledgerId + 1)
    when(blockDM.disseminateMessage(newEnvelope)).thenReturn(())
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
    val ledgerId = 1
    val me = NodeId(ByteString("Me"))
    val (core, consensusMap, txDM, _) = setupTest(ledgerId, me)
    val (_, testTxEnvelope, _) = setupMissingCapabilitiesTest(ledgerId, core, _ => true, me)
    val newEnvelope = testTxEnvelope.copy(ledgerId = ledgerId + 1)
    when(txDM.disseminateMessage(newEnvelope)).thenReturn(())
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
      implicit txSerializable: ByteStringSerializable[Tx],
      blockSerializable: ByteStringSerializable[Block[State, Header, Tx]]) = {
    val testTx = DummyTransaction(10)
    val testBlock = Block(DummyBlockHeader(1), immutable.Seq(testTx))
    val testBlockEnvelope = Envelope(testBlock, 1, destinationDescriptor)
    val testTxEnvelope = Envelope(testTx, 1, destinationDescriptor)
    val error = Left(new NetworkError {})
    (testBlockEnvelope, testTxEnvelope, error)
  }
}

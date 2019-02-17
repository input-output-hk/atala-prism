package io.iohk.cef.transactionservice
import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.query.LedgerQueryService
import io.iohk.cef.ledger.{Block, BlockHeader}
import io.iohk.network._
import io.iohk.cef.test.{DummyLedgerQuery, DummyTransaction}
import io.iohk.cef.transactionpool.TransactionPoolInterface
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, MustMatchers}

import scala.collection.immutable
import scala.concurrent.Future
import io.iohk.codecs.nio._

class NodeTransactionServiceSpec extends AsyncFlatSpec with MustMatchers with MockitoSugar {

  def mockConsensus: Consensus[String, DummyTransaction] =
    mock[Consensus[String, DummyTransaction]]

  def mockTxPoolFutureInterface: TransactionPoolInterface[String, DummyTransaction] =
    mock[TransactionPoolInterface[String, DummyTransaction]]

  def mockQueryService: LedgerQueryService[String, DummyLedgerQuery] = mock[LedgerQueryService[String, DummyLedgerQuery]]

  type State = String
  type Tx = DummyTransaction
  type BlockType = Block[State, Tx]

  def mockNetwork[M]: Network[M] = mock[Network[M]]

  def mockNioCodec: NioCodec[Envelope[Tx]] =
    mock[NioCodec[Envelope[Tx]]]

  def mockBlockSerializable: NioCodec[Envelope[BlockType]] =
    mock[NioCodec[Envelope[BlockType]]]

  private def setupTest(
      ledgerId: LedgerId,
      me: NodeId = NodeId("abcd")
  )(implicit txSerializable: NioCodec[Envelope[Tx]], blockSerializable: NioCodec[Envelope[Block[State, Tx]]]) = {
    val consensusMap = Map(ledgerId -> (mockTxPoolFutureInterface, mockConsensus, mockQueryService))
    val txDM = mockNetwork[Envelope[DummyTransaction]]
    val blockDM = mockNetwork[Envelope[Block[String, DummyTransaction]]]
    val txMessageStream = mock[MessageStream[Envelope[DummyTransaction]]]
    val blockMessageStream = mock[MessageStream[Envelope[Block[String, DummyTransaction]]]]
    when(txDM.messageStream).thenReturn(txMessageStream)
    when(blockDM.messageStream).thenReturn(blockMessageStream)
    when(txMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    when(blockMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    (
      new NodeTransactionServiceImpl(
        consensusMap,
        txDM,
        blockDM,
        me
      ),
      consensusMap,
      txDM,
      blockDM
    )
  }

  behavior of "NodeSpec"

  it should "receive a transaction" in {
    val testTx = DummyTransaction(10)
    val ledgerId = "1"
    val testEnvelope = Envelope(testTx, ledgerId, Everyone)
    implicit val bs1 = mockNioCodec
    implicit val bs2 = mockBlockSerializable
    val (transactionservice, consensusMap, txDM, _) = setupTest(ledgerId)
    when(consensusMap(ledgerId)._1.processTransaction(testEnvelope.content))
      .thenReturn(Right(()))
    transactionservice
      .receiveTransaction(testEnvelope)
      .map(r => {
        verify(txDM, times(1)).disseminateMessage(testEnvelope)
        verify(consensusMap(ledgerId)._1, times(1)).processTransaction(testEnvelope.content)
        r mustBe Right(())
      })
  }

  it should "receive a block" in {
    val testBlock =
      Block[String, DummyTransaction](BlockHeader(), immutable.Seq(DummyTransaction(10)))
    val ledgerId = "1"
    val testEnvelope = Envelope(testBlock, ledgerId, Everyone)
    implicit val bs1 = mockNioCodec
    implicit val bs2 = mockBlockSerializable
    val (transactionservice, consensusMap, _, blockDM) = setupTest(ledgerId)
    when(consensusMap(ledgerId)._2.process(testEnvelope.content))
      .thenReturn(Future.successful(Right(())))
    transactionservice
      .receiveBlock(testEnvelope)
      .map(r => {
        verify(blockDM, times(1)).disseminateMessage(testEnvelope)
        verify(consensusMap(ledgerId)._2, times(1)).process(testEnvelope.content)
        r mustBe Right(())
      })
  }

  it should "avoid processing a block if this is not a receiver" in {
    implicit val bs1 = mockNioCodec
    implicit val bs2 = mockBlockSerializable
    val ledgerId = "1"
    val me = NodeId("abcd")
    val (transactionservice, consensusMap, _, blockDM) = setupTest(ledgerId, me)
    val (testBlockEnvelope, _) = setupMissingCapabilitiesTest(ledgerId, transactionservice, Not(Everyone), me)
    for {
      rcv <- transactionservice.receiveBlock(testBlockEnvelope)
    } yield {
      verify(blockDM, times(1)).disseminateMessage(testBlockEnvelope)
      verify(consensusMap(ledgerId)._2, times(0)).process(testBlockEnvelope.content)
      rcv mustBe Right(())
    }
  }

  it should "avoid processing a tx if this is not a receiver" in {
    implicit val bs1 = mockNioCodec
    implicit val bs2 = mockBlockSerializable
    val ledgerId = "1"
    val me = NodeId("abcd")
    val (transactionservice, consensusMap, txDM, _) = setupTest(ledgerId, me)
    val (_, testTxEnvelope) = setupMissingCapabilitiesTest(ledgerId, transactionservice, Not(Everyone), me)
    for {
      rcv <- transactionservice.receiveTransaction(testTxEnvelope)
    } yield {
      verify(txDM, times(1)).disseminateMessage(testTxEnvelope)
      verify(consensusMap(ledgerId)._1, times(0)).processTransaction(testTxEnvelope.content)
      rcv mustBe Right(())
    }
  }

  it should "avoid processing a block if the node doesn't participate in consensus" in {
    implicit val bs1 = mockNioCodec
    implicit val bs2 = mockBlockSerializable
    val ledgerId = "1"
    val me = NodeId("abcd")
    val (transactionservice, consensusMap, _, blockDM) = setupTest(ledgerId, me)
    val (testBlockTxEnvelope, _) = setupMissingCapabilitiesTest(ledgerId, transactionservice, Everyone, me)
    val newEnvelope = testBlockTxEnvelope.copy(containerId = ledgerId + 1)
    for {
      rcv <- transactionservice.receiveBlock(newEnvelope)
    } yield {
      verify(blockDM, times(1)).disseminateMessage(newEnvelope)
      verify(consensusMap(ledgerId)._2, times(0)).process(newEnvelope.content)
      rcv mustBe Left(MissingCapabilitiesForTx(me, newEnvelope))
    }
  }

  it should "avoid processing a tx if the node doesn't participate in consensus" in {
    implicit val bs1 = mockNioCodec
    implicit val bs2 = mockBlockSerializable
    val ledgerId = "1"
    val me = NodeId("abcd")
    val (transactionservice, consensusMap, txDM, _) = setupTest(ledgerId, me)
    val (_, testTxEnvelope) = setupMissingCapabilitiesTest(ledgerId, transactionservice, Everyone, me)
    val newEnvelope = testTxEnvelope.copy(containerId = ledgerId + 1)
    for {
      rcv <- transactionservice.receiveTransaction(newEnvelope)
    } yield {
      verify(txDM, times(1)).disseminateMessage(newEnvelope)
      verify(consensusMap(ledgerId)._1, times(0)).processTransaction(newEnvelope.content)
      rcv mustBe Left(MissingCapabilitiesForTx(me, newEnvelope))
    }
  }

  private def setupMissingCapabilitiesTest(
      ledgerId: LedgerId,
      transactionservice: NodeTransactionService[String, DummyTransaction, DummyLedgerQuery],
      destinationDescriptor: DestinationDescriptor,
      me: NodeId
  )(implicit txSerializable: NioCodec[Envelope[Tx]], blockSerializable: NioCodec[Envelope[Block[State, Tx]]]) = {
    val testTx = DummyTransaction(10)
    val testBlock = Block[String, DummyTransaction](BlockHeader(), immutable.Seq(testTx))
    val testBlockEnvelope = Envelope(testBlock, "1", destinationDescriptor)
    val testTxEnvelope = Envelope(testTx, "1", destinationDescriptor)
    (testBlockEnvelope, testTxEnvelope)
  }
}

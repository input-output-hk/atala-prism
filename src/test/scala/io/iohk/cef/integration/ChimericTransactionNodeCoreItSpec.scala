package io.iohk.cef.integration

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import akka.util.Timeout
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.core.{Envelope, NodeCore}
import io.iohk.cef.frontend.controllers.ChimericTransactionsController
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.models.{CreateChimericTransactionRequest, CreateNonSignableChimericTransactionFragment}
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.network.{MessageStream, Network, NodeId}
import io.iohk.cef.transactionpool.TransactionPoolInterface
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import io.iohk.cef.codecs.nio._

class ChimericTransactionNodeCoreItSpec
    extends FlatSpec
    with MustMatchers
    with BeforeAndAfterAll
    with ScalaFutures
    with MockitoSugar
    with ScalatestRouteTest
    with PlayJsonSupport {

  import ChimericTransactionNodeCoreItSpec._
  import Codecs._

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  behavior of "ChimericTransactionNodeCoreItSpec"

  def createNodeCore: NodeCore[ChimericStateResult, ChimericTx] = {
    implicit val timeout = Timeout(10.seconds)
    implicit val envelopeSerializable = mock[NioEncDec[Envelope[TransactionType]]]
    implicit val blockSerializable = mock[NioEncDec[Envelope[BlockType]]]

    val generateHeader: Seq[TransactionType] => BlockHeader = _ => BlockHeader()

    implicit val blockSerializable2 = mock[NioEncDec[BlockType]]
    val ledgerStateStorage = mock[LedgerStateStorageType]

    val txPoolInterface =
      TransactionPoolInterface[TransactionStateType, TransactionType](
        generateHeader,
        10000,
        ledgerStateStorage,
        10.minutes)

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

    val consensusMap = Map("1" -> (txPoolInterface, consensus))

    val me = NodeId("3112")

    val core =
      new NodeCore(consensusMap, txNetwork, blockNetwork, me)(
        envelopeSerializable,
        blockSerializable,
        ExecutionContext.global)

    core.asInstanceOf[NodeCore[ChimericStateResult, ChimericTx]]
  }

  it should "process a transaction" in {
    val fragments = Seq(CreateNonSignableChimericTransactionFragment(CreateCurrency("BTC")))

    val node = createNodeCore
    val service = new ChimericTransactionService(node)
    val api = new ChimericTransactionsController(service)
    val routes = api.routes

    val entity = CreateChimericTransactionRequest(fragments, "1")
    val json = Json.toJson(entity)

    val request = Post("/chimeric-transactions", json)

    request ~> routes ~> check {
      status must ===(StatusCodes.Created)
    }
  }
}

object ChimericTransactionNodeCoreItSpec {

  type TransactionStateType = ChimericStateResult
  type TransactionType = Transaction[TransactionStateType]
  type BlockType = Block[TransactionStateType, TransactionType]

  type LedgerStateStorageType = LedgerStateStorage[TransactionStateType]

  type ConsensusType = Consensus[TransactionStateType, TransactionType]
}

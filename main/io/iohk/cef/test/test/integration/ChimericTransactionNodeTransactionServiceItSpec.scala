package io.iohk.cef.integration

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import akka.util.Timeout
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.transactionservice.{NodeTransactionService, NodeTransactionServiceImpl}
import io.iohk.cef.frontend.controllers.ChimericTransactionsController
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.models.{CreateChimericTransactionRequest, CreateNonSignableChimericTransactionFragment}
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.query.LedgerQueryService
import io.iohk.cef.ledger.query.chimeric.ChimericQuery
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.network.{Envelope, MessageStream, Network, NodeId}
import io.iohk.cef.transactionpool.TransactionPoolInterface
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import io.iohk.codecs.nio._
import org.mockito.ArgumentMatchers.any

class ChimericTransactionNodeTransactionServiceItSpec
    extends FlatSpec
    with MustMatchers
    with BeforeAndAfterAll
    with ScalaFutures
    with MockitoSugar
    with ScalatestRouteTest
    with PlayJsonSupport {

  import ChimericTransactionNodeTransactionServiceItSpec._
  import Codecs._

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  behavior of "ChimericTransactionNodeTransactionServiceItSpec"

  def createNodeTransactionService: NodeTransactionService[ChimericStateResult, ChimericTx, ChimericQuery] = {
    implicit val timeout = Timeout(10.seconds)
    implicit val envelopeSerializable = mock[NioCodec[Envelope[TransactionType]]]
    implicit val blockSerializable = mock[NioCodec[Envelope[BlockType]]]

    val generateHeader: Seq[TransactionType] => BlockHeader = _ => BlockHeader()

    implicit val blockSerializable2 = mock[NioCodec[BlockType]]
    val ledgerStateStorage = mock[LedgerStateStorageType[TransactionStateType]]
    implicit val transactionStateTypeEncDec = mock[NioCodec[TransactionStateType]]

    val txPoolInterface =
      TransactionPoolInterface[TransactionStateType, TransactionType](
        generateHeader,
        10000,
        ledgerStateStorage,
        10.minutes
      )

    val consensus = mock[ConsensusType]
    val blockNetwork = mock[Network[Envelope[BlockType]]]

    val mockTxMessageStream = mock[MessageStream[Envelope[TransactionType]]]

    val mockBlockMessageStream = mock[MessageStream[Envelope[BlockType]]]

    val txNetwork = mock[Network[Envelope[TransactionType]]]

    val queryService = mock[LedgerQueryService[ChimericStateResult, ChimericQuery]]

    when(ledgerStateStorage.slice(any())).thenReturn(new ChimericLedgerState(Map.empty))
    when(txNetwork.messageStream).thenReturn(mockTxMessageStream)
    when(blockNetwork.messageStream).thenReturn(mockBlockMessageStream)
    when(mockTxMessageStream.foreach(any())).thenReturn(Future.successful(()))
    when(mockBlockMessageStream.foreach(any())).thenReturn(Future.successful(()))

    val consensusMap = Map("1" -> (txPoolInterface, consensus, queryService))

    val me = NodeId("3112")

    val transactionservice =
      new NodeTransactionServiceImpl(consensusMap, txNetwork, blockNetwork, me)(
        envelopeSerializable,
        blockSerializable,
        ExecutionContext.global
      )

    transactionservice.asInstanceOf[NodeTransactionService[ChimericStateResult, ChimericTx, ChimericQuery]]
  }

  it should "process a transaction" in {
    val fragments = Seq(CreateNonSignableChimericTransactionFragment(CreateCurrency("BTC")))

    val node = createNodeTransactionService
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

object ChimericTransactionNodeTransactionServiceItSpec {

  type TransactionStateType = ChimericStateResult
  type TransactionType = Transaction[TransactionStateType]
  type BlockType = Block[TransactionStateType, TransactionType]

  type LedgerStateStorageType[S] = LedgerStateStorage[S]

  type ConsensusType = Consensus[TransactionStateType, TransactionType]
}

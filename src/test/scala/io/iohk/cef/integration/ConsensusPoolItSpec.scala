package io.iohk.cef.integration
import akka.actor.ActorSystem
import akka.testkit.TestKit
import io.iohk.cef.consensus.raft.Redirect
import io.iohk.cef.core.raftrpc.RaftRPCFactory
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.test.{DummyBlockHeader, DummyBlockSerializable, DummyTransaction}
import io.iohk.cef.transactionpool.{TimedQueue, TransactionPoolFutureInterface}
import org.scalatest.FlatSpecLike
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ConsensusPoolItSpec extends TestKit(ActorSystem("testActorModel")) with FlatSpecLike with MockitoSugar with TxPoolFixture {

  private def mockLedgerStateStorage[State] = mock[LedgerStateStorage[State]]
  behavior of "ConsensusPoolItSpec"

  it should "push periodical blocks to consensus" in {
    import io.iohk.cef.network.encoding.nio.NioCodecs._
    implicit val executionContext: ExecutionContext = system.dispatcher
    val mockNetworkDiscovery = mock[NetworkDiscovery]
    val mockTransports = mock[Transports]
    type B = Block[String, DummyBlockHeader, DummyTransaction]
    implicit val encoders = DummyBlockSerializable.serializable
    val raftFactory = new RaftRPCFactory[B](mockNetworkDiscovery, mockTransports)
    val ledgerStateStorage = mockLedgerStateStorage[String]
    val queue = TimedQueue[DummyTransaction]()
    val txPoolActorModelInterface =
      new TestableTransactionPoolActorModelInterface[String, DummyBlockHeader, DummyTransaction](
        system,
        txs => new DummyBlockHeader(txs.size),
        10000,
        ledgerStateStorage,
        1 minute,
        queue
      )
    val nodeId = "abcdef"
    val txPoolFutureInterface =
      new TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction](txPoolActorModelInterface)
    val clientCallback: Seq[B] => Future[Either[Redirect[B], Unit]] = (entries: Seq[B]) => {
      val result = entries.foldLeft[Future[Either[ApplicationError, Unit]]](Future.successful(Right(())))((s, e) =>{
        for {
          stateEither <- s
          elementEither <- txPoolFutureInterface.removeBlockTxs(e)
        } yield stateEither.flatMap(_ => elementEither)
      })
      result.map(_ match {
        case Left(error) => fail(s"Got message ${error}")
        case Right(_) => Right(())
      })
    }
    val raftInstance =
      raftFactory.apply(nodeId, null, null, clientCallback)
  }

}

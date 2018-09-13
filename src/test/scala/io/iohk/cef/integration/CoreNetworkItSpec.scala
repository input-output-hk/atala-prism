package io.iohk.cef.integration
import io.iohk.cef.consensus.MockingConsensus
import io.iohk.cef.core.Envelope
import io.iohk.cef.ledger.Block
import io.iohk.cef.network.{Network, NetworkFixture}
import io.iohk.cef.test.{DummyBlockHeader, DummyTransaction}
import io.iohk.cef.transactionpool.MockingTransactionPoolFutureInterface
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class CoreNetworkItSpec extends FlatSpec
  with MustMatchers
  with PropertyChecks
  with NetworkFixture
  with MockitoSugar
  with MockingTransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction]
  with MockingConsensus[String, DummyTransaction] {

  behavior of "CoreNetworkItSpec"

  val txNetwork = 1

  it should "receive a transaction" in {
    import io.iohk.cef.network.encoding.nio.NioCodecs._
    import io.iohk.cef.test.DummyBlockSerializable._
    val baseNetwork = randomBaseNetwork(None)
    val txNetwork = new Network[Envelope[DummyTransaction]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val blockNetwork = new Network[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]](baseNetwork.networkDiscovery, baseNetwork.transports)
//    val core = new NodeCore[String, DummyBlockHeader, DummyTransaction]()
  }

  it should "receive a block" in {
    pending
  }
}

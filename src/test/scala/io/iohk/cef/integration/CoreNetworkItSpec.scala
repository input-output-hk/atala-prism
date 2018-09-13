package io.iohk.cef.integration
import io.iohk.cef.network.NetworkFixture
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class CoreNetworkItSpec extends FlatSpec with MustMatchers with PropertyChecks with NetworkFixture {

  //Tests will come in a new PR

  behavior of "CoreNetworkItSpec"

  val txNetwork = 1

  it should "receive a transaction" in {
//    val baseNetwork = randomBaseNetwork(None)
//    val txNetwork = new Network[Envelope[DummyTransaction]](baseNetwork.transports, baseNetwork.networkDiscovery)
  }

  it should "receive a block" in {
    pending
  }
}

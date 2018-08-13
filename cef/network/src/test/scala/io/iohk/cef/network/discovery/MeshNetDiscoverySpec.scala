package io.iohk.cef.network.discovery
import org.scalatest.FlatSpec

class MeshNetDiscoverySpec extends FlatSpec {

  behavior of "MeshNetworkDiscovery"

  it should "have an initial routing table containg a bootstrap node" in pending

  it should "bootstrap with a self lookup" in pending

  it should "perform iterative lookups on the bootstrap result until converging" in pending


  it should "respond to a FindNodes request with the closest nodes in its routing table" in pending

  it should "response to a Ping request with a Pong" in pending

  it should "update the routing table if PeerInfo in a Ping contradicts it" in pending

  it should "update the routing table if PeerInfo in a FindNodes contradicts it" in pending

  it should "update the routing table if a peer fails to respond to a Ping" in pending

  it should "update the routing table if a peer fails to respond to a FindNodes" in pending
}

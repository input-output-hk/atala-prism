package io.iohk.cef.network.discovery

import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.transport.tcp.NetUtils.{aRandomAddress, aRandomNodeId}
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration
import io.iohk.cef.network.{ConversationalNetworkConfiguration, PeerInfo}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class MeshNetDiscoverySpec extends FlatSpec {

  behavior of "MeshNetworkDiscovery"

  ignore should "have an initial routing table containing itself and a bootstrap node" in {
    val peerInfo =
      PeerInfo(aRandomNodeId(), ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(aRandomAddress()))))

    val transports = new Transports(peerInfo)

    val bootstrapPeerInfo =
      PeerInfo(aRandomNodeId(), ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(aRandomAddress()))))

    val discovery = new MeshNetDiscovery(peerInfo, bootstrapPeerInfo, transports)

    discovery.nearestPeerTo(peerInfo.nodeId) shouldBe Some(peerInfo)
    discovery.nearestPeerTo(bootstrapPeerInfo.nodeId) shouldBe Some(bootstrapPeerInfo)
  }

  it should "bootstrap with a self lookup" in pending

  it should "perform iterative lookups on the bootstrap result until converging" in pending

  it should "respond to a FindNodes request with the closest nodes in its routing table" in pending

  it should "response to a Ping request with a Pong" in pending

  it should "update the routing table if PeerInfo in a Ping contradicts it" in pending

  it should "update the routing table if PeerInfo in a FindNodes contradicts it" in pending

  it should "update the routing table if a peer fails to respond to a Ping" in pending

  it should "update the routing table if a peer fails to respond to a FindNodes" in pending
}

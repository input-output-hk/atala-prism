package io.iohk.cef.utils

import java.net.InetSocketAddress

import io.iohk.cef.discovery.DiscoveryMessage.Capabilities
import io.iohk.cef.network._
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ECPublicKeyParameters

sealed trait ServerStatus
object ServerStatus {
  case object NotListening extends ServerStatus
  case class Listening(address: InetSocketAddress) extends ServerStatus
}

case class NodeStatus(
                       key: AsymmetricCipherKeyPair,
                       capabilities: Capabilities,
                       serverStatus: ServerStatus) {

  val nodeId = key.getPublic.asInstanceOf[ECPublicKeyParameters].toNodeId
}

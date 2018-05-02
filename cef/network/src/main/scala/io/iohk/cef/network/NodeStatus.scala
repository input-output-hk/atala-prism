package io.iohk.cef.network

import java.net.InetSocketAddress

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

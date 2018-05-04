package io.iohk.cef.network

import io.iohk.cef.encoding.rlp.{RLPEncDec, RLPEncodeable, RLPException, RLPList}

case class Node(address: NodeAddress, capabilities: Capabilities)

object Node {

  implicit def nodeRlpEncDec(implicit
                            nodeAddrEncDec: RLPEncDec[NodeAddress],
                             capEncDec: RLPEncDec[Capabilities]) = new RLPEncDec[Node] {
    override def encode(obj: Node): RLPEncodeable =
      RLPList(nodeAddrEncDec.encode(obj.address), capEncDec.encode(obj.capabilities))

    override def decode(rlp: RLPEncodeable): Node = rlp match {
      case RLPList(addr, cap) =>
        Node(nodeAddrEncDec.decode(addr), capEncDec.decode(cap))
      case _ => throw new RLPException("src is not a Node")
    }
  }
}

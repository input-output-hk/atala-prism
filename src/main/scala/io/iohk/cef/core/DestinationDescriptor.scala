package io.iohk.cef.core
import io.iohk.cef.network.NodeId
import io.iohk.cef.codecs.nio._

sealed trait DestinationDescriptor {
  def apply(v1: NodeId): Boolean
}
object DestinationDescriptor {
  implicit val DestinationDescriptorNioEncDec: NioEncDec[DestinationDescriptor] = {
    import io.iohk.cef.codecs.nio.auto._
    val e: NioEncoder[DestinationDescriptor] = genericEncoder
    val d: NioDecoder[DestinationDescriptor] = genericDecoder
    NioEncDec(e, d)
  }
}

case object Everyone extends DestinationDescriptor {
  override def apply(v1: NodeId): Boolean = true
}

case class SingleNode(nodeId: NodeId) extends DestinationDescriptor {
  override def apply(v1: NodeId): Boolean = nodeId == v1
}

case class SetOfNodes(set: Set[NodeId]) extends DestinationDescriptor {
  override def apply(v1: NodeId): Boolean = set.contains(v1)
}

case class Not(destinationDescriptor: DestinationDescriptor) extends DestinationDescriptor {
  override def apply(v1: NodeId): Boolean = !destinationDescriptor(v1)
}

case class And(a: DestinationDescriptor, b: DestinationDescriptor) extends DestinationDescriptor {
  override def apply(v1: NodeId): Boolean = a(v1) && b(v1)
}

case class Or(a: DestinationDescriptor, b: DestinationDescriptor) extends DestinationDescriptor {
  override def apply(v1: NodeId): Boolean = a(v1) || b(v1)
}

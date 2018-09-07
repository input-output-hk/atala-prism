package io.iohk.cef.core
import akka.util.ByteString
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.network.NodeId
import io.iohk.cef.protobuf.DestinationDescriptor._
import io.iohk.cef.protobuf.DestinationDescriptor.DestinationDescriptorProto.Fragment._

sealed trait DestinationDescriptor extends (NodeId => Boolean)

case class Anyone() extends DestinationDescriptor {
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

object DestinationDescriptor {
  import io.iohk.cef.utils.ProtoBufByteStringConversion._
  val destinationDescriptorSerializer: ByteStringSerializable[DestinationDescriptor] = new ByteStringSerializable[DestinationDescriptor] {

    override def serialize(t: DestinationDescriptor): ByteString = {
      ByteString(toDestinationDescriptorProto(t).toByteArray)
    }

    override def deserialize(bytes: ByteString): DestinationDescriptor = {
      fromDestinationDescriptorProto(DestinationDescriptorProto.parseFrom(bytes.toArray))
    }
  }

  def toDestinationDescriptorProto(t: DestinationDescriptor): DestinationDescriptorProto = {
    val predicate = t match {
      case Anyone() => AnyoneWrapper(AnyoneProto())
      case SingleNode(nodeId) => SingleNodeWrapper(SingleNodeProto(nodeId.id))
      case SetOfNodes(set) =>
        SetOfNodesWrapper(SetOfNodesProto(set.map(nodeId => akkaByteStringToProtoByteString(nodeId.id)).toSeq))
      case Not(destinationDescriptor) => NotWrapper(NotProto(toDestinationDescriptorProto(destinationDescriptor)))
      case And(a, b) => AndWrapper(AndProto(toDestinationDescriptorProto(a), toDestinationDescriptorProto(b)))
      case Or(a, b) => OrWrapper(OrProto(toDestinationDescriptorProto(a), toDestinationDescriptorProto(b)))
    }
    DestinationDescriptorProto(predicate)
  }

  def fromDestinationDescriptorProto(parsed: DestinationDescriptorProto): DestinationDescriptor = {
    if(parsed.fragment.isAnyoneWrapper) {
      Anyone()
    } else if(parsed.fragment.isSingleNodeWrapper) {
      SingleNode(NodeId(parsed.getSingleNodeWrapper.nodeId))
    } else if(parsed.fragment.isSetOfNodesWrapper) {
      SetOfNodes(parsed.getSetOfNodesWrapper.nodeId.toSet.map((bs: com.google.protobuf.ByteString) =>
        NodeId(bs)
      ))
    } else if(parsed.fragment.isNotWrapper) {
      Not(fromDestinationDescriptorProto(parsed.getNotWrapper.destinationDescriptor))
    } else if(parsed.fragment.isAndWrapper) {
      And(fromDestinationDescriptorProto(parsed.getAndWrapper.left),
        fromDestinationDescriptorProto(parsed.getAndWrapper.right))
    } else if(parsed.fragment.isOrWrapper) {
      Or(fromDestinationDescriptorProto(parsed.getOrWrapper.left),
        fromDestinationDescriptorProto(parsed.getOrWrapper.right))
    } else {
      throw new IllegalArgumentException(s"Unexpected parsed proto: ${parsed}")
    }
  }
}

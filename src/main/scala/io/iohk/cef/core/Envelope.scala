package io.iohk.cef.core
import akka.util.ByteString
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.protobuf.Envelope.EnvelopeProto

/**
  *
  * @param transaction
  * @param ledgerId
  * @param destinationDescriptor a function from NodeInfo to Boolean expressing which nodes are intended to process this tx
  *                              It is the user's responsibility to validate that the destination nodes can handle the
  *                              ledger with id ledgerId
  * @tparam State the ledgerState
  */
case class Envelope[+D](content: D, ledgerId: LedgerId, destinationDescriptor: DestinationDescriptor)

object Envelope {
  import io.iohk.cef.utils.ProtoBufByteStringConversion._

  implicit def envelopeSerializer[T](
      implicit contentSerializer: ByteStringSerializable[T]): ByteStringSerializable[Envelope[T]] =
    new ByteStringSerializable[Envelope[T]] {
      override def serialize(t: Envelope[T]): ByteString = {
        val proto = EnvelopeProto(
          contentSerializer.serialize(t.content),
          t.ledgerId,
          DestinationDescriptor.toDestinationDescriptorProto(t.destinationDescriptor)
        )
        ByteString(proto.toByteArray)
      }
      override def deserialize(bytes: ByteString): Envelope[T] = {
        val parsed = EnvelopeProto.parseFrom(bytes.toArray)
        Envelope(
          contentSerializer.deserialize(parsed.content),
          parsed.ledgerId,
          DestinationDescriptor.fromDestinationDescriptorProto(parsed.destinationDescriptor)
        )
      }
    }
}

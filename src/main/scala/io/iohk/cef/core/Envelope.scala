package io.iohk.cef.core
import akka.util.ByteString
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.protobuf.Envelope.EnvelopeProto

import scala.util.Try

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
      override def encode(t: Envelope[T]): ByteString = {
        val proto = EnvelopeProto(
          contentSerializer.encode(t.content),
          t.ledgerId,
          DestinationDescriptor.toDestinationDescriptorProto(t.destinationDescriptor)
        )
        ByteString(proto.toByteArray)
      }
      override def decode(bytes: ByteString): Option[Envelope[T]] = {
        for {
          parsed <- Try(EnvelopeProto.parseFrom(bytes.toArray)).toOption
          decoded <- contentSerializer.decode(parsed.content)
        } yield
          Envelope(
            decoded,
            parsed.ledgerId,
            DestinationDescriptor.fromDestinationDescriptorProto(parsed.destinationDescriptor)
          )
      }
    }
}

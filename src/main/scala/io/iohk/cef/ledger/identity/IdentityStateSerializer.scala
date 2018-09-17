package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.ledger.identity.storage.protobuf.IdentityLedgerState.PublicKeyListProto

object IdentityStateSerializer {

  implicit val byteStringSerializable = new ByteStringSerializable[Set[SigningPublicKey]] {

    override def deserialize(bytes: ByteString): Set[SigningPublicKey] = {
      val parsed = PublicKeyListProto.parseFrom(bytes.toArray)
      parsed.publicKeys.flatMap { bs =>
        SigningPublicKey
          .decodeFrom(ByteString(bs.toByteArray))
          .left
          .map { e =>
            throw new RuntimeException(s"Unable to decode signing public key: $e")
          }
          .toOption
      }.toSet
    }

    override def serialize(t: Set[SigningPublicKey]): ByteString = {
      ByteString(
        PublicKeyListProto(
          t.toSeq.map(bs => com.google.protobuf.ByteString.copyFrom(bs.toByteString.toArray))
        ).toByteArray)
    }
  }
}

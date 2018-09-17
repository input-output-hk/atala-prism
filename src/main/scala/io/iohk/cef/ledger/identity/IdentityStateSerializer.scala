package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.ledger.identity.storage.protobuf.IdentityLedgerState.PublicKeyListProto

import scala.util.Try

object IdentityStateSerializer {

  implicit val byteStringSerializable = new ByteStringSerializable[Set[SigningPublicKey]] {

    override def decode(bytes: ByteString): Option[Set[SigningPublicKey]] = {
      for {
        parsed <- Try(PublicKeyListProto.parseFrom(bytes.toArray)).toOption
      } yield {
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
    }

    override def encode(t: Set[SigningPublicKey]): ByteString = {
      ByteString(
        PublicKeyListProto(
          t.toSeq.map(bs => com.google.protobuf.ByteString.copyFrom(bs.toByteString.toArray))
        ).toByteArray)
    }
  }
}

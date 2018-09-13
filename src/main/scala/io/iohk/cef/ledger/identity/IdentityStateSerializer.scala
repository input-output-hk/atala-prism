package io.iohk.cef.ledger.identity

import java.security.PublicKey

import akka.util.ByteString
import io.iohk.cef.crypto.low.decodePublicKey
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.ledger.identity.storage.protobuf.IdentityLedgerState.PublicKeyListProto

import scala.util.Try

object IdentityStateSerializer {

  implicit val byteStringSerializable = new ByteStringSerializable[Set[PublicKey]] {

    override def decode(bytes: ByteString): Option[Set[PublicKey]] = {
      for {
        parsed <- Try(PublicKeyListProto.parseFrom(bytes.toArray)).toOption
      } yield parsed.publicKeys.map(bs => decodePublicKey(bs.toByteArray)).toSet
    }

    override def encode(t: Set[PublicKey]): ByteString = {
      ByteString(
        PublicKeyListProto(
          t.toSeq.map(bs => com.google.protobuf.ByteString.copyFrom(bs.getEncoded))
        ).toByteArray)
    }
  }
}

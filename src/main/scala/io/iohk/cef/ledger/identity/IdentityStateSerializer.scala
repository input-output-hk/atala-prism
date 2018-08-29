package io.iohk.cef.ledger.identity

import java.security.PublicKey

import akka.util.ByteString
import io.iohk.cef.crypto.low.decodePublicKey
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.ledger.identity.storage.protobuf.IdentityLedgerState.PublicKeyListProto

object IdentityStateSerializer {

  implicit val byteStringSerializable = new ByteStringSerializable[Set[PublicKey]] {

    override def deserialize(bytes: ByteString): Set[PublicKey] = {
      val parsed = PublicKeyListProto.parseFrom(bytes.toArray)
      parsed.publicKeys.map(bs => decodePublicKey(bs.toByteArray)).toSet
    }

    override def serialize(t: Set[PublicKey]): ByteString = {
      ByteString(PublicKeyListProto(
        t.toSeq.map(bs => com.google.protobuf.ByteString.copyFrom(bs.getEncoded))
      ).toByteArray)
    }
  }
}

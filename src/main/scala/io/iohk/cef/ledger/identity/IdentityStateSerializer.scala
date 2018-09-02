package io.iohk.cef.ledger.identity
import akka.util.ByteString
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.ledger.identity.storage.protobuf.IdentityLedgerState.PublicKeyListProto

object IdentityStateSerializer {
  implicit val byteStringSerializable = new ByteStringSerializable[Set[ByteString]] {

    override def deserialize(bytes: ByteString): Set[ByteString] = {
      val parsed = PublicKeyListProto.parseFrom(bytes.toArray)
      parsed.publicKeys.map(bs => ByteString(bs.toByteArray)).toSet
    }

    override def serialize(t: Set[ByteString]): ByteString = {
      ByteString(
        PublicKeyListProto(
          t.toSeq.map(bs => com.google.protobuf.ByteString.copyFrom(bs.toArray))
        ).toByteArray)
    }
  }
}

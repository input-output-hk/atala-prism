package io.iohk.atala.prism.vault

import tofu.logging.derivation.loggable
import derevo.derive
import io.iohk.atala.prism.protos.vault_models
import com.google.protobuf.ByteString

package object model {
  final case class CreateRecord(
      type_ : Record.Type,
      id: Record.Id,
      payload: Record.Payload
  )

  final case class Record(
      type_ : Record.Type,
      id: Record.Id,
      payload: Record.Payload
  ) {
    def toProto: vault_models.EncryptedRecord =
      vault_models.EncryptedRecord(
        ByteString.copyFrom(type_.encrypted.toArray),
        ByteString.copyFrom(id.encrypted.toArray),
        ByteString.copyFrom(payload.encrypted.toArray)
      )
  }

  object Record {
    @derive(loggable)
    case class Type private (encrypted: Vector[Byte])

    object Type {
      def unsafeFrom(bytes: Array[Byte]): Type =
        if (bytes.nonEmpty) new Type(bytes.toVector) {}
        else throw new IllegalArgumentException("Empty record type")
    }

    @derive(loggable)
    case class Id private (encrypted: Vector[Byte])

    object Id {
      def unsafeFrom(bytes: Array[Byte]): Id =
        if (bytes.nonEmpty) new Id(bytes.toVector) {}
        else throw new IllegalArgumentException("Empty record id")
    }

    @derive(loggable)
    case class Payload private (encrypted: Vector[Byte])

    object Payload {
      def unsafeFrom(bytes: Array[Byte]): Payload =
        if (bytes.nonEmpty) new Payload(bytes.toVector) {}
        else throw new IllegalArgumentException("Empty record payload")
    }
  }
}

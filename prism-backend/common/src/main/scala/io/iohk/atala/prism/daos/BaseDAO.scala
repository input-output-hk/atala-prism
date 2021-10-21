package io.iohk.atala.prism.daos

import doobie.util.invariant.InvalidEnum
import doobie.{Get, Meta, Put}
import io.circe.Json
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.crypto.{Sha256Digest => SHA256Digest}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.{Ledger, TransactionId, UUIDValue}

import java.util.UUID
import io.iohk.atala.prism.credentials.CredentialBatchId

trait BaseDAO {
  implicit val uuidMeta: Meta[UUID] = doobie.postgres.implicits.UuidType
  implicit val bigIntMeta: Meta[BigInt] = implicitly[Meta[BigDecimal]].timap(_.toBigInt)(BigDecimal.apply)

  implicit val jsonPut: Put[Json] = doobie.postgres.circe.json.implicits.jsonPut
  implicit val jsonGet: Get[Json] = doobie.postgres.circe.json.implicits.jsonGet

  implicit val sha256MetaBytes: Meta[SHA256Digest] = Meta[Array[Byte]].timap { value =>
    SHA256Digest.fromBytes(value)
  }(_.getValue)

  implicit val atalaOperationIdMeta: Meta[AtalaOperationId] =
    Meta[Array[Byte]]
      .timap(value => AtalaOperationId.fromVectorUnsafe(value.toVector))(_.value.toArray)

  implicit val ecPublicKeyMeta: Meta[ECPublicKey] =
    Meta[Array[Byte]].timap(b => EC.toPublicKeyFromCompressed(b))(_.getEncodedCompressed)

  implicit val transactionIdMeta: Meta[TransactionId] =
    Meta[Array[Byte]].timap(b =>
      TransactionId.from(b).getOrElse(throw new IllegalArgumentException("Unexpected TransactionId"))
    )(_.value.toArray)

  implicit val ledgerMeta: Meta[Ledger] =
    Meta[String].timap(b => Ledger.withNameInsensitiveOption(b).getOrElse(throw InvalidEnum[Ledger](b)))(_.entryName)

  // it makes no sense to register an unpublished DID, we'd always look for the canonical DID
  implicit val didMeta: Meta[DID] = Meta[String].timap(DID.fromString) { did =>
    Option(did.asCanonical()).getOrElse(throw new RuntimeException(s"Invalid canonical DID: $did")).getValue
  }

  protected def uuidValueMeta[T <: UUIDValue](builder: UUIDValue.Builder[T]): Meta[T] = {
    Meta[UUID].timap(builder.apply)(_.uuid)
  }

  implicit val credentialBatchId: Meta[CredentialBatchId] =
    Meta[String].timap(x =>
      Option(
        CredentialBatchId
          .fromString(x)
      ).getOrElse(throw new RuntimeException(s"Invalid batch id: $x"))
    )(_.getId)
}

object BaseDAO extends BaseDAO

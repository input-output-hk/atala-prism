package io.iohk.atala.prism.node.repositories.daos

import doobie.util.invariant.InvalidEnum
import doobie.{Get, Meta, Put}
import io.circe.Json
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.models.{AtalaOperationId, Ledger, TransactionId, UUIDValue}

import java.util.UUID

trait BaseDAO {
  implicit val uuidMeta: Meta[UUID] = doobie.postgres.implicits.UuidType
  implicit val bigIntMeta: Meta[BigInt] =
    implicitly[Meta[BigDecimal]].timap(_.toBigInt)(BigDecimal.apply)

  implicit val jsonPut: Put[Json] = doobie.postgres.circe.json.implicits.jsonPut
  implicit val jsonGet: Get[Json] = doobie.postgres.circe.json.implicits.jsonGet

  implicit val sha256MetaBytes: Meta[Sha256Hash] = Meta[Array[Byte]].timap { value =>
    Sha256Hash.fromBytes(value)
  }(_.bytes.toArray)

  implicit val atalaOperationIdMeta: Meta[AtalaOperationId] =
    Meta[Array[Byte]]
      .timap(value => AtalaOperationId.fromVectorUnsafe(value.toVector))(
        _.value.toArray
      )

  implicit val transactionIdMeta: Meta[TransactionId] =
    Meta[Array[Byte]].timap(b =>
      TransactionId
        .from(b)
        .getOrElse(
          throw new IllegalArgumentException("Unexpected TransactionId")
        )
    )(_.value.toArray)

  implicit val ledgerMeta: Meta[Ledger] =
    Meta[String].timap(b =>
      Ledger
        .withNameInsensitiveOption(b)
        .getOrElse(throw InvalidEnum[Ledger](b))
    )(_.entryName)

  // it makes no sense to register an unpublished DID, we'd always look for the canonical DID
  implicit val didMeta: Meta[DID] = Meta[String].timap(DID.fromString) { did =>
    Option(did.asCanonical())
      .getOrElse(throw new RuntimeException(s"Invalid canonical DID: $did"))
      .value
  }

  protected def uuidValueMeta[T <: UUIDValue](
      builder: UUIDValue.Builder[T]
  ): Meta[T] = {
    Meta[UUID].timap(builder.apply)(_.uuid)
  }
}

object BaseDAO extends BaseDAO

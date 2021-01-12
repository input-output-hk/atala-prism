package io.iohk.atala.prism.daos

import java.util.UUID

import doobie.util.invariant.InvalidEnum
import doobie.{Get, Meta, Put}
import io.circe.Json
import io.iohk.atala.prism.crypto.{EC, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{Ledger, TransactionId}

trait BaseDAO {
  implicit val uuidMeta: Meta[UUID] = doobie.postgres.implicits.UuidType
  implicit val bigIntMeta: Meta[BigInt] = implicitly[Meta[BigDecimal]].timap(_.toBigInt)(BigDecimal.apply)

  implicit val jsonPut: Put[Json] = doobie.postgres.circe.json.implicits.jsonPut
  implicit val jsonGet: Get[Json] = doobie.postgres.circe.json.implicits.jsonGet

  implicit val sha256Meta: Meta[SHA256Digest] = Meta[String].timap(SHA256Digest.fromHex)(_.hexValue)
  implicit val ecPublicKeyMeta: Meta[ECPublicKey] =
    Meta[Array[Byte]].timap(b => EC.toPublicKey(b))(_.getEncoded)

  implicit val transactionIdMeta: Meta[TransactionId] =
    Meta[Array[Byte]].timap(b =>
      TransactionId.from(b).getOrElse(throw new IllegalArgumentException("Unexpected TransactionId"))
    )(_.value.toArray)

  implicit val ledgerMeta: Meta[Ledger] =
    Meta[String].timap(b => Ledger.withNameInsensitiveOption(b).getOrElse(throw InvalidEnum[Ledger](b)))(_.entryName)

  implicit val didMeta: Meta[DID] =
    Meta[String].timap(s => {
      DID.unsafeFromString(s)
    })(_.value)
}

object BaseDAO extends BaseDAO

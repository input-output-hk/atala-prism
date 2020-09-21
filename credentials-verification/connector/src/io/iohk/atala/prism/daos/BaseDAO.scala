package io.iohk.atala.prism.daos

import java.util.UUID

import doobie.util.{Get, Meta, Put}
import io.circe.Json
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.cmanager.models
import io.iohk.atala.prism.cmanager.models._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.ParticipantId

trait BaseDAO {
  implicit val uuidMeta: Meta[UUID] = doobie.postgres.implicits.UuidType
  implicit val bigIntMeta: Meta[BigInt] = implicitly[Meta[BigDecimal]].timap(_.toBigInt())(BigDecimal.apply)
  implicit val participantIdMeta: Meta[ParticipantId] = uuidMeta.timap(ParticipantId.apply)(_.uuid)
  implicit val connectionIdMeta: Meta[ConnectionId] = uuidMeta.timap(ConnectionId.apply)(_.id)
  implicit val groupNameMeta: Meta[IssuerGroup.Name] = Meta[String].timap(IssuerGroup.Name.apply)(_.value)
  implicit val groupIdMeta: Meta[IssuerGroup.Id] = uuidMeta.timap(IssuerGroup.Id.apply)(_.value)
  implicit val credentialIdMeta: Meta[UniversityCredential.Id] = uuidMeta.timap(UniversityCredential.Id.apply)(_.value)
  implicit val studentIdMeta: Meta[Student.Id] = uuidMeta.timap(Student.Id.apply)(_.value)
  implicit val studentConnectionStatusMeta: Meta[Student.ConnectionStatus] =
    Meta[String].timap(Student.ConnectionStatus.withNameInsensitive)(_.entryName)

  implicit val jsonPut: Put[Json] = doobie.postgres.circe.json.implicits.jsonPut
  implicit val jsonGet: Get[Json] = doobie.postgres.circe.json.implicits.jsonGet

  implicit val sha256Meta: Meta[SHA256Digest] = Meta[String].timap(SHA256Digest.fromHex)(_.hexValue)
  implicit val genericCredentialIdMeta: Meta[models.GenericCredential.Id] =
    uuidMeta.timap(GenericCredential.Id.apply)(_.value)
}

object BaseDAO extends BaseDAO

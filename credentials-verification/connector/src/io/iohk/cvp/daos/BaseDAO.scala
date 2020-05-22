package io.iohk.cvp.daos

import java.util.UUID

import doobie.util.{Get, Meta, Put}
import io.circe.Json
import io.iohk.connector.model.ConnectionId
import io.iohk.cvp.cmanager.models._
import io.iohk.cvp.models.ParticipantId

trait BaseDAO {
  implicit val uuidMeta: Meta[UUID] = doobie.postgres.implicits.UuidType
  implicit val bigIntMeta: Meta[BigInt] = implicitly[Meta[BigDecimal]].timap(_.toBigInt())(BigDecimal.apply)
  implicit val participantIdMeta: Meta[ParticipantId] = uuidMeta.timap(ParticipantId.apply)(_.uuid)
  implicit val connectionIdMeta: Meta[ConnectionId] = uuidMeta.timap(ConnectionId.apply)(_.id)
  implicit val subjectIdMeta: Meta[Subject.Id] = uuidMeta.timap(Subject.Id.apply)(_.value)
  implicit val groupNameMeta: Meta[IssuerGroup.Name] = Meta[String].timap(IssuerGroup.Name.apply)(_.value)
  implicit val groupIdMeta: Meta[IssuerGroup.Id] = uuidMeta.timap(IssuerGroup.Id.apply)(_.value)
  implicit val credentialIdMeta: Meta[UniversityCredential.Id] = uuidMeta.timap(UniversityCredential.Id.apply)(_.value)
  implicit val issuerIdMeta: Meta[Issuer.Id] = uuidMeta.timap(Issuer.Id.apply)(_.value)
  implicit val studentIdMeta: Meta[Student.Id] = uuidMeta.timap(Student.Id.apply)(_.value)
  implicit val studentConnectionStatusMeta: Meta[Student.ConnectionStatus] =
    Meta[String].timap(Student.ConnectionStatus.withNameInsensitive)(_.entryName)

  implicit val jsonPut: Put[Json] = doobie.postgres.circe.json.implicits.jsonPut
  implicit val jsonGet: Get[Json] = doobie.postgres.circe.json.implicits.jsonGet
}

object BaseDAO extends BaseDAO

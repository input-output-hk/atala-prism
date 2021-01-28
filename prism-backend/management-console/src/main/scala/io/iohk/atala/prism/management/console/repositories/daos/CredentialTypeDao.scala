package io.iohk.atala.prism.management.console.repositories.daos

import java.time.Instant
import java.util.UUID
import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.implicits.legacy.instant._
import doobie.util.update.Update
import io.iohk.atala.prism.management.console.models.{
  CreateCredentialType,
  CredentialType,
  CredentialTypeField,
  CredentialTypeFieldId,
  CredentialTypeId,
  CredentialTypeState,
  CredentialTypeWithRequiredFields,
  ParticipantId
}

object CredentialTypeDao {

  def create(
      createCredentialType: CreateCredentialType
  ): ConnectionIO[CredentialTypeWithRequiredFields] = {
    (for {
      credentialType <- CredentialTypeDao.insertCredentialType(createCredentialType)
      credentialTypeRequiredFields = createCredentialType.fields.map { typeField =>
        CredentialTypeField(
          id = CredentialTypeFieldId(UUID.randomUUID()),
          credentialTypeId = credentialType.id,
          name = typeField.name,
          description = typeField.description
        )
      }
      _ <- CredentialTypeDao.insertCredentialTypeFields(credentialTypeRequiredFields)
      requiredFields <- findRequiredFields(credentialType.id)
    } yield (CredentialTypeWithRequiredFields(credentialType, requiredFields)))
  }

  def insertCredentialType(createCredentialType: CreateCredentialType): ConnectionIO[CredentialType] = {
    sql"""
         |INSERT INTO credential_types
         |  (credential_type_id, name, institution_id, state, template, created_at)
         |VALUES
         |  (
         |    ${CredentialTypeId(UUID.randomUUID())},
         |    ${createCredentialType.name},
         |    ${createCredentialType.institution},
         |    ${CredentialTypeState.Draft.entryName}::CREDENTIAL_TYPE_STATE,
         |    ${createCredentialType.template},
         |    ${Instant.now()}
         |  )
         |RETURNING credential_type_id, name, institution_id, state, template, created_at
         |""".stripMargin.query[CredentialType].unique
  }

  def insertCredentialTypeField(credentialTypeField: CredentialTypeField): ConnectionIO[Int] =
    insertManyCredentialTypeField.toUpdate0(credentialTypeField).run

  def insertCredentialTypeFields(credentialTypeField: List[CredentialTypeField]): ConnectionIO[Int] =
    insertManyCredentialTypeField.updateMany(credentialTypeField)

  val insertManyCredentialTypeField: Update[CredentialTypeField] =
    Update[CredentialTypeField](
      """INSERT INTO
        | credential_type_fields(credential_type_field_id, credential_type_id, name, description)
        | values (?, ?, ?, ?)""".stripMargin
    )

  def findCredentialType(credentialTypeId: CredentialTypeId): doobie.ConnectionIO[Option[CredentialType]] = {
    sql"""
         |SELECT credential_type_id, name, institution_id, state, template, created_at
         |FROM credential_types
         |WHERE credential_type_id = ${credentialTypeId}
         |""".stripMargin.query[CredentialType].option
  }

  def findCredentialType(institution: ParticipantId, name: String): doobie.ConnectionIO[Option[CredentialType]] = {
    sql"""
         |SELECT credential_type_id, name, institution_id, state, template, created_at
         |FROM credential_types
         |WHERE institution_id = $institution AND
         |      name = $name
         |""".stripMargin.query[CredentialType].option
  }

  def findCredentialTypes(institution: ParticipantId): doobie.ConnectionIO[List[CredentialType]] = {
    sql"""
         |SELECT credential_type_id, name, institution_id, state, template, created_at
         |FROM credential_types
         |WHERE institution_id = $institution
         |ORDER BY created_at
         |""".stripMargin.query[CredentialType].to[List]
  }

  def findRequiredFields(credentialTypeId: CredentialTypeId): doobie.ConnectionIO[List[CredentialTypeField]] = {
    sql"""
         |SELECT credential_type_field_id, credential_type_id, name, description
         |FROM credential_type_fields
         |WHERE credential_type_id = ${credentialTypeId}
         |ORDER BY name
         |""".stripMargin.query[CredentialTypeField].to[List]
  }

}

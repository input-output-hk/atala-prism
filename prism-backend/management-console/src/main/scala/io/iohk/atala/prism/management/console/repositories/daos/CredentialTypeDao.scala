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
  ParticipantId,
  UpdateCredentialType
}
import doobie.free.connection

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

  def update(
      updateCredentialType: UpdateCredentialType
  ): ConnectionIO[Unit] = {
    for {
      resultCount <- CredentialTypeDao.updateCredentialType(updateCredentialType)
      _ <-
        if (resultCount != 1)
          connection.raiseError(
            new Exception(
              s"CredentialTypeDao: cannot update credential type, update result count was not equel to 1: $resultCount"
            )
          )
        else
          connection.pure(())
      _ <- deleteCredentialTypeFields(updateCredentialType.id)
      credentialTypeRequiredFields = updateCredentialType.fields.map { typeField =>
        CredentialTypeField(
          id = CredentialTypeFieldId(UUID.randomUUID()),
          credentialTypeId = updateCredentialType.id,
          name = typeField.name,
          description = typeField.description
        )
      }
      _ <- CredentialTypeDao.insertCredentialTypeFields(credentialTypeRequiredFields)
    } yield ()
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

  def updateCredentialType(updateCredentialType: UpdateCredentialType): ConnectionIO[Int] = {
    sql"""
         | UPDATE credential_types SET
         | name = ${updateCredentialType.name},
         | template = ${updateCredentialType.template}
         | WHERE credential_type_id = ${updateCredentialType.id}
    """.stripMargin.update.run
  }

  def markAsReady(credentialTypeId: CredentialTypeId): ConnectionIO[Int] = {
    sql"""
         | UPDATE credential_types SET
         | state = ${CredentialTypeState.Ready.entryName}::CREDENTIAL_TYPE_STATE
         | WHERE credential_type_id = ${credentialTypeId}
    """.stripMargin.update.run
  }

  def markAsArchived(credentialTypeId: CredentialTypeId): ConnectionIO[Int] = {
    sql"""
         | UPDATE credential_types SET
         | state = ${CredentialTypeState.Archived.entryName}::CREDENTIAL_TYPE_STATE
         | WHERE credential_type_id = ${credentialTypeId}
    """.stripMargin.update.run
  }

  def deleteCredentialTypeFields(credentialTypeId: CredentialTypeId): ConnectionIO[Int] =
    sql"DELETE FROM credential_type_fields WHERE credential_type_id = ${credentialTypeId}".update.run

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

  def findCredentialType(
      institution: ParticipantId,
      credentialTypeId: CredentialTypeId
  ): doobie.ConnectionIO[Option[CredentialType]] = {
    sql"""
         |SELECT credential_type_id, name, institution_id, state, template, created_at
         |FROM credential_types
         |WHERE institution_id = $institution AND
         |      credential_type_id = ${credentialTypeId}
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

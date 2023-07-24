package io.iohk.atala.prism.management.console.repositories.daos

import cats.data.OptionT

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
import cats.implicits._
import io.iohk.atala.prism.management.console.config.DefaultCredentialTypeConfig
import io.iohk.atala.prism.management.console.errors.{
  CredentialTypeDoesNotBelongToInstitution,
  CredentialTypeDoesNotExist,
  ManagementConsoleError
}
import io.scalaland.chimney.dsl._

object CredentialTypeDao {

  def insertDefaultCredentialTypes(
      participantId: ParticipantId,
      defaultCredentialTypeConfig: DefaultCredentialTypeConfig
  ): ConnectionIO[List[CredentialTypeWithRequiredFields]] = {
    defaultCredentialTypeConfig.defaultCredentialTypes
      .map { defaultCredentialType =>
        defaultCredentialType
          .into[CreateCredentialType]
          .withFieldConst(_.icon, None)
          .transform
      }
      .map(create(participantId, _))
      .sequence
  }

  def create(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): ConnectionIO[CredentialTypeWithRequiredFields] = {
    (for {
      credentialType <- CredentialTypeDao.insertCredentialType(
        participantId,
        createCredentialType
      )
      credentialTypeRequiredFields = createCredentialType.fields.map { typeField =>
        CredentialTypeField(
          id = CredentialTypeFieldId(UUID.randomUUID()),
          credentialTypeId = credentialType.id,
          name = typeField.name,
          description = typeField.description,
          `type` = typeField.`type`
        )
      }
      _ <- CredentialTypeDao.insertCredentialTypeFields(
        credentialTypeRequiredFields
      )
      requiredFields <- findRequiredFields(credentialType.id)
    } yield (CredentialTypeWithRequiredFields(credentialType, requiredFields)))
  }

  def update(
      updateCredentialType: UpdateCredentialType
  ): ConnectionIO[Unit] = {
    for {
      resultCount <- CredentialTypeDao.updateCredentialType(
        updateCredentialType
      )
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
          description = typeField.description,
          `type` = typeField.`type`
        )
      }
      _ <- CredentialTypeDao.insertCredentialTypeFields(
        credentialTypeRequiredFields
      )
    } yield ()
  }

  def findValidated[A](
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): ConnectionIO[
    Either[ManagementConsoleError, CredentialTypeWithRequiredFields]
  ] = {
    withRequiredFields(CredentialTypeDao.findCredentialType(credentialTypeId))
      .map {
        case None =>
          Left(CredentialTypeDoesNotExist(credentialTypeId))

        case Some(credentialTypeWithRequiredFields)
            if (credentialTypeWithRequiredFields.credentialType.institution != institutionId) =>
          Left(
            CredentialTypeDoesNotBelongToInstitution(
              credentialTypeId,
              institutionId
            )
          )

        case Some(credentialTypeWithRequiredFields) =>
          Right(credentialTypeWithRequiredFields)
      }
  }

  def withRequiredFields(
      credentialTypeQuery: doobie.ConnectionIO[Option[CredentialType]]
  ): ConnectionIO[Option[CredentialTypeWithRequiredFields]] = {
    (for {
      credentialType <- OptionT(credentialTypeQuery)
      requiredFields <- OptionT.liftF(
        CredentialTypeDao.findRequiredFields(credentialType.id)
      )
    } yield CredentialTypeWithRequiredFields(
      credentialType,
      requiredFields
    )).value
  }

  def insertCredentialType(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): ConnectionIO[CredentialType] = {
    sql"""
         |INSERT INTO credential_types
         |  (credential_type_id, name, institution_id, state, template, created_at, icon)
         |VALUES
         |  (
         |    ${CredentialTypeId(UUID.randomUUID())},
         |    ${createCredentialType.name},
         |    $participantId,
         |    ${CredentialTypeState.Draft.entryName}::CREDENTIAL_TYPE_STATE,
         |    ${createCredentialType.template},
         |    ${Instant.now()},
         |    ${createCredentialType.icon}
         |  )
         |RETURNING credential_type_id, name, institution_id, state, template, created_at, icon
         |""".stripMargin.query[CredentialType].unique
  }

  def updateCredentialType(
      updateCredentialType: UpdateCredentialType
  ): ConnectionIO[Int] = {
    sql"""
         | UPDATE credential_types SET
         | name = ${updateCredentialType.name},
         | template = ${updateCredentialType.template},
         | icon = ${updateCredentialType.icon}
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

  def deleteCredentialTypeFields(
      credentialTypeId: CredentialTypeId
  ): ConnectionIO[Int] =
    sql"DELETE FROM credential_type_fields WHERE credential_type_id = ${credentialTypeId}".update.run

  def insertCredentialTypeField(
      credentialTypeField: CredentialTypeField
  ): ConnectionIO[Int] =
    insertManyCredentialTypeField.toUpdate0(credentialTypeField).run

  def insertCredentialTypeFields(
      credentialTypeField: List[CredentialTypeField]
  ): ConnectionIO[Int] =
    insertManyCredentialTypeField.updateMany(credentialTypeField)

  val insertManyCredentialTypeField: Update[CredentialTypeField] =
    Update[CredentialTypeField](
      """INSERT INTO
        | credential_type_fields(credential_type_field_id, credential_type_id, name, description, type)
        | values (?, ?, ?, ?, ?::CREDENTIAL_TYPE_FIELD_TYPE)""".stripMargin
    )

  def findCredentialType(
      credentialTypeId: CredentialTypeId
  ): doobie.ConnectionIO[Option[CredentialType]] = {
    sql"""
         |SELECT credential_type_id, name, institution_id, state, template, created_at, icon
         |FROM credential_types
         |WHERE credential_type_id = ${credentialTypeId}
         |""".stripMargin.query[CredentialType].option
  }

  def findCredentialType(
      institution: ParticipantId,
      name: String
  ): doobie.ConnectionIO[Option[CredentialType]] = {
    sql"""
         |SELECT credential_type_id, name, institution_id, state, template, created_at, icon
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
         |SELECT credential_type_id, name, institution_id, state, template, created_at, icon
         |FROM credential_types
         |WHERE institution_id = $institution AND
         |      credential_type_id = ${credentialTypeId}
         |""".stripMargin.query[CredentialType].option
  }

  def findCredentialTypes(
      institution: ParticipantId
  ): doobie.ConnectionIO[List[CredentialType]] = {
    sql"""
         |SELECT credential_type_id, name, institution_id, state, template, created_at, icon
         |FROM credential_types
         |WHERE institution_id = $institution
         |ORDER BY created_at
         |""".stripMargin.query[CredentialType].to[List]
  }

  def findRequiredFields(
      credentialTypeId: CredentialTypeId
  ): doobie.ConnectionIO[List[CredentialTypeField]] = {
    sql"""
         |SELECT credential_type_field_id, credential_type_id, name, description, type
         |FROM credential_type_fields
         |WHERE credential_type_id = ${credentialTypeId}
         |ORDER BY name
         |""".stripMargin.query[CredentialTypeField].to[List]
  }

}

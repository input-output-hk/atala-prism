package io.iohk.atala.prism.management.console.repositories.daos

import java.util.UUID
import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator

import io.iohk.atala.prism.management.console.models.{
  ParticipantId,
  CreateCredentialTypeCategory,
  CredentialTypeCategory,
  CredentialTypeCategoryId,
  CredentialTypeCategoryState
}

object CredentialTypeCategoryDao {

  def create(
      participantId: ParticipantId,
      createCredentialTypeCategory: CreateCredentialTypeCategory
  ): ConnectionIO[CredentialTypeCategory] = {
    sql"""
         |INSERT INTO credential_type_categories
         |  (credential_type_category_id, name, state, institution_id)
         |VALUES
         |  (
         |    ${CredentialTypeCategoryId(UUID.randomUUID())},
         |    ${createCredentialTypeCategory.name},
         |    ${createCredentialTypeCategory.state.entryName}::CREDENTIAL_TYPE_CATEGORY_STATE,
         |    $participantId
         |  )
         |RETURNING credential_type_category_id, name, institution_id, state
         |""".stripMargin.query[CredentialTypeCategory].unique
  }

  def find(participantId: ParticipantId): ConnectionIO[List[CredentialTypeCategory]] = {
    sql"""
         |SELECT credential_type_category_id, name, institution_id, state
         |FROM credential_type_categories
         |WHERE institution_id = $participantId
         |""".stripMargin.query[CredentialTypeCategory].to[List]
  }

  def updateState(
      credentialTypeCategoryId: CredentialTypeCategoryId,
      state: CredentialTypeCategoryState
  ): ConnectionIO[CredentialTypeCategory] = {
    sql"""
         | UPDATE credential_type_categories SET
         | state = ${state.entryName}::CREDENTIAL_TYPE_CATEGORY_STATE
         | WHERE credential_type_category_id = ${credentialTypeCategoryId}
         | 
         | RETURNING credential_type_category_id, name, institution_id, state
    """.stripMargin.query[CredentialTypeCategory].unique
  }

}

package io.iohk.atala.prism.management.console.migrations

import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.repositories.PostgresMigrationSpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import org.postgresql.util.PSQLException

import java.time.Instant
import java.util.UUID

class V18MigrationSpec extends PostgresMigrationSpec("V18") with BaseDAO {
  val credentialTypeCategoryId = UUID.randomUUID()
  val issuerId = UUID.randomUUID()
  val credentialTypeId = UUID.randomUUID()

  def prepare(): Unit = {
    createParticipant()
    createCredentialTypeCategory()
  }

  def createCredentialTypeCategory(): Unit = {
    sql"""INSERT INTO credential_type_categories (credential_type_category_id, name, state)
         |VALUES ($credentialTypeCategoryId, 'Some credential type category', 'DRAFT')""".stripMargin
      .runUpdate()
  }
  def createParticipant(): Unit = {
    sql"""INSERT INTO participants (participant_id, name, did, created_at)
         |VALUES ($issuerId, 'The Issuer', 'did:prism:theissuer', ${Instant.now})""".stripMargin
      .runUpdate()
  }

  def createCredentialType(): Unit = {
    sql"""INSERT INTO credential_types (credential_type_id, name, institution_id, state, template, created_at, credential_type_category_id)
         |VALUES ($credentialTypeId, 'The Credential Type', $issuerId, 'DRAFT', 'a template', ${Instant.now}, $credentialTypeCategoryId)""".stripMargin
      .runUpdate()
  }
  def getCategoryIdOfCredentialType(): UUID = {
    sql"SELECT credential_type_category_id FROM credential_types WHERE credential_type_id = $credentialTypeId"
      .runUnique[UUID]()
  }

  test(
    beforeApply = {
      prepare()
      val thrown = intercept[PSQLException] {
        createCredentialType()
      }
      thrown.getMessage.contains(
        "column \"credential_type_category_id\" of relation \"credential_types\" does not exist"
      ) mustBe true
    },
    afterApplied = {
      createCredentialType()
      getCategoryIdOfCredentialType() mustBe credentialTypeCategoryId
    }
  )

}

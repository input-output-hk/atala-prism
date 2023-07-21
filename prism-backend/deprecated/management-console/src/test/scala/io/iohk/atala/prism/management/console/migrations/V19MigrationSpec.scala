package io.iohk.atala.prism.management.console.migrations

import doobie.implicits._
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.repositories.PostgresMigrationSpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits

import java.util.UUID

class V19MigrationSpec extends PostgresMigrationSpec("V19") with BaseDAO {
  val credentialTypeCategoryId = UUID.randomUUID()

  def prepare(): Unit = {
    createCredentialTypeCategory()
  }

  def createCredentialTypeCategory(): Unit = {
    sql"""INSERT INTO credential_type_categories (credential_type_category_id, name, state)
         |VALUES ($credentialTypeCategoryId, 'Some credential type category', 'DRAFT')""".stripMargin
      .runUpdate()
  }

  def getInstitutionIdOfCredentialTypeCategory(): Option[UUID] = {
    sql"SELECT institution_id FROM credential_type_categories WHERE credential_type_category_id = $credentialTypeCategoryId"
      .runUnique[Option[UUID]]()
  }

  test(
    beforeApply = {
      prepare()
    },
    afterApplied = {
      getInstitutionIdOfCredentialTypeCategory() mustBe None
    }
  )

}

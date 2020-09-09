package io.iohk.atala.prism.connector.repositories.migrations

import java.util.UUID

import doobie.implicits._
import io.iohk.atala.prism.cmanager.models.{Student, Subject}
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import io.iohk.atala.prism.connector.repositories.helpers.DataHelper._
import io.iohk.atala.prism.repositories.PostgresMigrationSpec

class V20MigrationSpec extends PostgresMigrationSpec("V20") {

  test(
    beforeApply = {
      val issuerId = createIssuer("test-issuer")
      val group = createGroup(issuerId, "test-group")
      val subjectId = Subject.Id(UUID.randomUUID())
      val status: Student.ConnectionStatus = Student.ConnectionStatus.InvitationMissing
      val _ = sql"""
             |INSERT INTO issuer_subjects (subject_id, created_at, connection_status, group_id, subject_data)
             |VALUES($subjectId, NOW(), $status::STUDENT_CONNECTION_STATUS_TYPE, ${group.id}, jsonb_build_object('other_id', 'none'))""".stripMargin
        .runUpdate()
    },
    afterApplied = { _: Unit =>
      ()
    }
  )
}

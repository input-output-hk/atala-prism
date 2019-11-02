package io.iohk.cvp.cmanager.repositories.daos

import java.util.UUID

import doobie.implicits._
import io.iohk.cvp.cmanager.models.Credential
import io.iohk.cvp.cmanager.models.requests.CreateCredential

object CredentialsDAO {

  def create(data: CreateCredential): doobie.ConnectionIO[Credential] = {
    val id = Credential.Id(UUID.randomUUID())
    sql"""
         |INSERT INTO credentials (credential_id, issued_by, subject, title, enrollment_date, graduation_date, group_name)
         |VALUES ($id, ${data.issuedBy}, ${data.subject}, ${data.title}, ${data.enrollmentDate}, ${data.graduationDate}, ${data.groupName})
         |""".stripMargin.update.run
      .map(_ => Credential.create(id, data))
  }
}

package io.iohk.atala.prism.cmanager

import java.time.LocalDate

import io.circe.Json
import doobie.implicits._
import io.iohk.atala.prism.cmanager.models.IssuerGroup
import io.iohk.atala.prism.cmanager.models.Subject.ExternalId
import io.iohk.atala.prism.cmanager.models.requests.CreateSubject
import io.iohk.atala.prism.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.atala.prism.cmanager.repositories.common.DataPreparation.{createIssuer, createIssuerGroup}
import io.iohk.atala.prism.cmanager.repositories.daos.{IssuerGroupsDAO, IssuerSubjectsDAO}

class IntegrityTriggersSpec extends CManagerRepositorySpec {

  "contacts_per_group" should {
    "fail insertion when a subject and group do not belong to the same issuer" in {
      val issuer1 = createIssuer("Issuer-1").id
      val issuer2 = createIssuer("Issuer-2").id
      val group = createIssuerGroup(issuer1, IssuerGroup.Name("Grp 1"))
      val externalId = ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateSubject(issuer2, externalId, json)

      val query = for {
        subject <- IssuerSubjectsDAO.create(request)
        _ <- IssuerGroupsDAO.addSubject(group.id, subject.id)
      } yield subject

      val exc = intercept[Exception](
        query.transact(database).unsafeToFuture().futureValue
      )

      // we need to check that the error is the one we recognise with the trigger
      exc.getMessage.contains("ERROR: The group and contact do not belong to the same issuer") must be(true)
    }
  }
}

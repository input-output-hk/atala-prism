package io.iohk.atala.prism.cmanager

import java.time.LocalDate

import io.circe.Json
import doobie.implicits._
import io.iohk.atala.prism.cmanager.models.IssuerGroup
import io.iohk.atala.prism.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.atala.prism.cmanager.repositories.common.DataPreparation.{createIssuer, createIssuerGroup}
import io.iohk.atala.prism.cmanager.repositories.daos.IssuerGroupsDAO
import io.iohk.atala.prism.console.models.{Contact, CreateContact}
import io.iohk.atala.prism.console.repositories.daos.ContactsDAO

class IntegrityTriggersSpec extends CManagerRepositorySpec {

  "contacts_per_group" should {
    "fail insertion when a subject and group do not belong to the same issuer" in {
      val issuerId1 = createIssuer("Issuer-1")
      val issuerId2 = createIssuer("Issuer-2")
      val group = createIssuerGroup(issuerId1, IssuerGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(
        issuerId2,
        externalId,
        json
      )

      val query = for {
        contact <- ContactsDAO.createContact(request)
        _ <- IssuerGroupsDAO.addContact(group.id, contact.id)
      } yield contact

      val exc = intercept[Exception](
        query.transact(database).unsafeToFuture().futureValue
      )

      // we need to check that the error is the one we recognise with the trigger
      exc.getMessage.contains("ERROR: The group and contact do not belong to the same issuer") must be(true)
    }
  }
}

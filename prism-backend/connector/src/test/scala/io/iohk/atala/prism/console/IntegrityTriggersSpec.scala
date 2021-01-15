package io.iohk.atala.prism.console

import java.time.LocalDate

import doobie.implicits._
import io.circe.Json
import DataPreparation.{createIssuer, createIssuerGroup}
import io.iohk.atala.prism.console.models.{Contact, CreateContact, IssuerGroup}
import io.iohk.atala.prism.console.repositories.daos.{ContactsDAO, IssuerGroupsDAO}
import io.iohk.atala.prism.AtalaWithPostgresSpec

import scala.concurrent.duration._

class IntegrityTriggersSpec extends AtalaWithPostgresSpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)

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
        _ <- IssuerGroupsDAO.addContact(group.id, contact.contactId)
      } yield contact

      val exc = intercept[Exception](
        query.transact(database).unsafeToFuture().futureValue
      )

      // we need to check that the error is the one we recognise with the trigger
      exc.getMessage.contains("ERROR: The group and contact do not belong to the same issuer") must be(true)
    }
  }
}

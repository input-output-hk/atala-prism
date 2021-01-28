package io.iohk.atala.prism.management.console

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.{
  ContactsDAO,
  CredentialTypeDao,
  CredentialsDAO,
  InstitutionGroupsDAO,
  ParticipantsDAO
}

import java.time.{Instant, LocalDate}
import scala.util.Random

object DataPreparation {
  def createParticipant(name: String)(implicit database: Transactor[IO]): ParticipantId = {
    val curatedName = name.filter(c => c.isLetterOrDigit || c == '.' || c == '-' || c == '_')
    createParticipant(name, DID.buildPrismDID(curatedName))
  }

  def createParticipant(
      name: String,
      did: DID
  )(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    val id = ParticipantId.random()
    val participant = ParticipantInfo(id, name, did, None)
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()
    id
  }

  def createInstitutionGroup(institutionId: ParticipantId, name: InstitutionGroup.Name)(implicit
      database: Transactor[IO]
  ): InstitutionGroup = {
    InstitutionGroupsDAO.create(institutionId, name).transact(database).unsafeRunSync()
  }

  def createContact(
      institutionId: ParticipantId,
      name: String = s"name-${Random.nextInt(100)}",
      groupName: Option[InstitutionGroup.Name] = None,
      createdAt: Option[Instant] = None,
      externalId: Contact.ExternalId = Contact.ExternalId.random()
  )(implicit
      database: Transactor[IO]
  ): Contact = {
    val request = CreateContact(
      createdBy = institutionId,
      data = Json.obj(
        "email" -> "donthaveone@here.com".asJson,
        "admissionDate" -> LocalDate.now().asJson
      ),
      externalId = externalId,
      name = name
    )

    groupName match {
      case None =>
        ContactsDAO.createContact(request, createdAt.getOrElse(Instant.now())).transact(database).unsafeRunSync()
      case Some(name) =>
        val group = InstitutionGroupsDAO
          .find(institutionId, name)
          .transact(database)
          .unsafeRunSync()
          .getOrElse(throw new RuntimeException(s"Group $name does not exist"))

        val query = for {
          contact <- ContactsDAO.createContact(request, createdAt.getOrElse(Instant.now()))
          _ <- InstitutionGroupsDAO.addContact(group.id, contact.contactId)
        } yield contact

        query.transact(database).unsafeRunSync()
    }
  }

  def createGenericCredential(
      issuedBy: ParticipantId,
      subjectId: Contact.Id,
      tag: String = "",
      credentialIssuanceContactId: Option[CredentialIssuance.ContactId] = None
  )(implicit
      database: Transactor[IO]
  ): GenericCredential = {
    val request = CreateGenericCredential(
      issuedBy = issuedBy,
      subjectId = subjectId,
      credentialData = Json.obj(
        "title" -> s"Major In Applied Blockchain $tag".trim.asJson,
        "enrollmentDate" -> LocalDate.now().asJson,
        "graduationDate" -> LocalDate.now().plusYears(5).asJson
      ),
      credentialIssuanceContactId = credentialIssuanceContactId
    )

    CredentialsDAO.create(request).transact(database).unsafeRunSync()
  }

  def createCredentialType(institutionId: ParticipantId, name: String)(implicit
      database: Transactor[IO]
  ): CredentialTypeWithRequiredFields = {
    CredentialTypeDao.create(sampleCreateCredentialType(institutionId, name)).transact(database).unsafeRunSync()
  }

  def sampleCreateCredentialType(institutionId: ParticipantId, name: String): CreateCredentialType = {
    CreateCredentialType(
      name = name,
      institution = institutionId,
      template = "",
      fields = List(
        CreateCredentialTypeField(
          name = "name1",
          description = "description1"
        ),
        CreateCredentialTypeField(
          name = "name2",
          description = "description2"
        )
      )
    )
  }
}

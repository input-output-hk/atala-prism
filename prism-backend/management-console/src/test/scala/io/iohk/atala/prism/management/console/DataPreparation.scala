package io.iohk.atala.prism.management.console

import java.time.LocalDate

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.{
  ContactsDAO,
  CredentialsDAO,
  InstitutionGroupsDAO,
  ParticipantsDAO
}

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
      subjectName: String,
      groupName: InstitutionGroup.Name,
      tag: String = ""
  )(implicit
      database: Transactor[IO]
  ): Contact = createContact(institutionId, subjectName, Some(groupName), tag)

  def createContact(
      institutionId: ParticipantId,
      subjectName: String,
      groupName: Option[InstitutionGroup.Name],
      tag: String
  )(implicit
      database: Transactor[IO]
  ): Contact = {
    val request = CreateContact(
      createdBy = institutionId,
      data = Json.obj(
        "universityAssignedId" -> s"uid - $tag".asJson,
        "full_name" -> subjectName.asJson,
        "email" -> "donthaveone@here.com".asJson,
        "admissionDate" -> LocalDate.now().asJson
      ),
      externalId = Contact.ExternalId.random()
    )

    groupName match {
      case None =>
        ContactsDAO.createContact(request).transact(database).unsafeRunSync()
      case Some(name) =>
        val group = InstitutionGroupsDAO
          .find(institutionId, name)
          .transact(database)
          .unsafeRunSync()
          .getOrElse(throw new RuntimeException(s"Group $name does not exist"))

        val query = for {
          contact <- ContactsDAO.createContact(request)
          _ <- InstitutionGroupsDAO.addContact(group.id, contact.contactId)
        } yield contact

        query.transact(database).unsafeRunSync()
    }
  }

  def createGenericCredential(issuedBy: ParticipantId, subjectId: Contact.Id, tag: String = "")(implicit
      database: Transactor[IO]
  ): GenericCredential = {
    val request = CreateGenericCredential(
      issuedBy = issuedBy,
      subjectId = subjectId,
      credentialData = Json.obj(
        "title" -> s"Major IN Applied Blockchain $tag".trim.asJson,
        "enrollmentDate" -> LocalDate.now().asJson,
        "graduationDate" -> LocalDate.now().plusYears(5).asJson
      )
    )

    CredentialsDAO.create(request).transact(database).unsafeRunSync()
  }
}

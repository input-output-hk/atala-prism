package io.iohk.atala.prism.console

import java.time.LocalDate
import java.util.UUID

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.console.repositories.{daos => consoleDaos}
import io.iohk.atala.prism.connector.model.{ParticipantInfo, ParticipantType, TokenString}
import io.iohk.atala.prism.connector.repositories.{daos => connectorDaos}
import io.iohk.atala.prism.console.models.{
  Contact,
  CreateContact,
  CreateGenericCredential,
  GenericCredential,
  Institution,
  IssuerGroup
}
import io.iohk.atala.prism.models.ParticipantId

object DataPreparation {

  import consoleDaos._
  import connectorDaos._

  def createIssuer(name: String = "Issuer", tag: String = "")(implicit database: Transactor[IO]): Institution.Id = {
    val id = Institution.Id(UUID.randomUUID())
    val did = s"did:geud:issuer-x$tag"
    // dirty hack to create a participant while creating an issuer, TODO: Merge the tables
    val participant =
      ParticipantInfo(ParticipantId(id.value), ParticipantType.Issuer, None, name, Option(did), None, None, None)
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()

    id
  }

  def createIssuerGroup(issuerId: Institution.Id, name: IssuerGroup.Name)(implicit
      database: Transactor[IO]
  ): IssuerGroup = {
    IssuerGroupsDAO.create(issuerId, name).transact(database).unsafeRunSync()
  }

  // Generic versions
  def createGenericCredential(issuedBy: Institution.Id, subjectId: Contact.Id, tag: String = "")(implicit
      database: Transactor[IO]
  ): GenericCredential = {
    val request = CreateGenericCredential(
      issuedBy = issuedBy,
      subjectId = subjectId,
      credentialData = Json.obj(
        "title" -> s"Major IN Applied Blockchain $tag".trim.asJson,
        "enrollmentDate" -> LocalDate.now().asJson,
        "graduationDate" -> LocalDate.now().plusYears(5).asJson
      ),
      groupName = s"Computer Science $tag".trim
    )

    CredentialsDAO.create(request).transact(database).unsafeRunSync()
  }

  def createContact(issuerId: Institution.Id, subjectName: String, groupName: IssuerGroup.Name, tag: String = "")(
      implicit database: Transactor[IO]
  ): Contact = createContact(issuerId, subjectName, Some(groupName), tag)

  def createContact(issuerId: Institution.Id, subjectName: String, groupName: Option[IssuerGroup.Name], tag: String)(
      implicit database: Transactor[IO]
  ): Contact = {
    val request = CreateContact(
      createdBy = issuerId,
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
        val group = IssuerGroupsDAO
          .find(issuerId, name)
          .transact(database)
          .unsafeRunSync()
          .getOrElse(throw new RuntimeException(s"Group $name does not exist"))

        val query = for {
          contact <- ContactsDAO.createContact(request)
          _ <- IssuerGroupsDAO.addContact(group.id, contact.contactId)
        } yield contact

        query.transact(database).unsafeRunSync()
    }
  }

  def generateConnectionToken(issuerId: Institution.Id, contactId: Contact.Id, maybeToken: Option[TokenString] = None)(
      implicit database: Transactor[IO]
  ): TokenString = {
    val token = maybeToken.getOrElse(TokenString.random())

    val tx = for {
      _ <- ConnectionTokensDAO.insert(ParticipantId(issuerId.value), token)
      _ <- ContactsDAO.setConnectionToken(issuerId, contactId, token)
    } yield token

    tx.transact(database).unsafeRunSync()
  }
}

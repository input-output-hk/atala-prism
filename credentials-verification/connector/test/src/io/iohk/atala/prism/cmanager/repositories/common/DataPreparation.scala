package io.iohk.atala.prism.cmanager.repositories.common

import java.time.LocalDate
import java.util.UUID

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.connector.model.{ParticipantInfo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.{daos => connectorDaos}
import io.iohk.atala.prism.cmanager.models.requests.{CreateGenericCredential, CreateStudent, CreateUniversityCredential}
import io.iohk.atala.prism.cmanager.models._
import io.iohk.atala.prism.cmanager.repositories.{daos => cmanagerDaos}
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.daos.{ContactsDAO, IssuerGroupsDAO}
import io.iohk.atala.prism.models.ParticipantId

object DataPreparation {

  import cmanagerDaos._
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

  def createCredential(issuedBy: Institution.Id, studentId: Student.Id, tag: String = "")(implicit
      database: Transactor[IO]
  ): UniversityCredential = {
    val request = CreateUniversityCredential(
      issuedBy = issuedBy,
      studentId = studentId,
      title = s"Major IN Applied Blockchain $tag".trim,
      enrollmentDate = LocalDate.now(),
      graduationDate = LocalDate.now().plusYears(5),
      groupName = s"Computer Science $tag".trim
    )

    CredentialsDAO.createUniversityCredential(request).transact(database).unsafeRunSync()
  }

  def createStudent(issuer: Institution.Id, name: String, groupName: IssuerGroup.Name, tag: String = "")(implicit
      database: Transactor[IO]
  ): Student = createStudent(issuer, name, Some(groupName), tag)

  def createStudent(issuerId: Institution.Id, name: String, groupName: Option[IssuerGroup.Name], tag: String)(implicit
      database: Transactor[IO]
  ): Student = {
    val request = CreateStudent(
      issuer = issuerId,
      universityAssignedId = s"uid - $tag",
      fullName = name,
      email = "donthaveone@here.com",
      admissionDate = LocalDate.now()
    )

    groupName match {
      case None =>
        StudentsDAO.createStudent(request).transact(database).unsafeRunSync()
      case Some(name) =>
        val group = IssuerGroupsDAO
          .find(issuerId, name)
          .transact(database)
          .unsafeRunSync()
          .getOrElse(throw new RuntimeException("Missing group"))

        val query = for {
          student <- StudentsDAO.createStudent(request)
          _ <- IssuerGroupsDAO.addContact(group.id, Contact.Id(student.id.value))
        } yield student

        query.transact(database).unsafeRunSync()
    }
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

  def createSubject(issuerId: Institution.Id, subjectName: String, groupName: IssuerGroup.Name, tag: String = "")(
      implicit database: Transactor[IO]
  ): Contact = createSubject(issuerId, subjectName, Some(groupName), tag)

  def createSubject(issuerId: Institution.Id, subjectName: String, groupName: Option[IssuerGroup.Name], tag: String)(
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
          _ <- IssuerGroupsDAO.addContact(group.id, contact.id)
        } yield contact

        query.transact(database).unsafeRunSync()
    }
  }
}

package io.iohk.cvp.cmanager.repositories.common

import java.time.LocalDate
import java.util.UUID

import cats.effect.IO
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.{GenericCredential, Issuer, IssuerGroup, Student, Subject, UniversityCredential}
import io.iohk.cvp.cmanager.repositories.{daos => cmanagerDaos}
import io.iohk.connector.repositories.{daos => connectorDaos}
import doobie.implicits._
import io.circe.Json
import io.circe.Json.JString
import io.iohk.connector.model.{ParticipantInfo, ParticipantType}
import io.iohk.cvp.cmanager.models.requests.{
  CreateGenericCredential,
  CreateStudent,
  CreateSubject,
  CreateUniversityCredential
}
import io.iohk.cvp.models.ParticipantId
import io.circe.syntax._

object DataPreparation {

  import cmanagerDaos._
  import connectorDaos._

  def createIssuer(name: String = "Issuer", tag: String = "")(implicit database: Transactor[IO]): Issuer = {
    val id = Issuer.Id(UUID.randomUUID())
    val did = s"did:geud:issuer-x$tag"
    // dirty hack to create a participant while creating an issuer, TODO: Merge the tables
    val participant = ParticipantInfo(ParticipantId(id.value), ParticipantType.Issuer, None, name, Option(did), None)
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()
    IssuersDAO.insert(Issuer(id)).transact(database).unsafeRunSync()

    Issuer(id)
  }

  def createCredential(issuedBy: Issuer.Id, studentId: Student.Id, tag: String = "")(implicit
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

  def createStudent(issuer: Issuer.Id, name: String, groupName: IssuerGroup.Name, tag: String = "")(implicit
      database: Transactor[IO]
  ): Student = {
    val group = IssuerGroupsDAO
      .find(issuer, groupName)
      .transact(database)
      .unsafeRunSync()
      .getOrElse(throw new RuntimeException("Missing group"))

    val request = CreateStudent(
      issuer = issuer,
      universityAssignedId = s"uid - $tag",
      fullName = name,
      email = "donthaveone@here.com",
      admissionDate = LocalDate.now(),
      groupName = group.name
    )

    IssuerSubjectsDAO.create(request, group.id).transact(database).unsafeRunSync()
  }

  def createIssuerGroup(issuer: Issuer.Id, name: IssuerGroup.Name)(implicit database: Transactor[IO]): IssuerGroup = {
    IssuerGroupsDAO.create(issuer, name).transact(database).unsafeRunSync()
  }

  // Generic versions
  def createGenericCredential(issuedBy: Issuer.Id, subjectId: Subject.Id, tag: String = "")(implicit
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

  def createSubject(issuerId: Issuer.Id, subjectName: String, groupName: IssuerGroup.Name, tag: String = "")(implicit
      database: Transactor[IO]
  ): Subject = {
    val group = IssuerGroupsDAO
      .find(issuerId, groupName)
      .transact(database)
      .unsafeRunSync()
      .getOrElse(throw new RuntimeException("Missing group"))

    val request = CreateSubject(
      issuerId = issuerId,
      data = Json.obj(
        "universityAssignedId" -> s"uid - $tag".asJson,
        "full_name" -> subjectName.asJson,
        "email" -> "donthaveone@here.com".asJson,
        "admissionDate" -> LocalDate.now().asJson
      ),
      groupName = group.name
    )

    IssuerSubjectsDAO.create(request, group.id).transact(database).unsafeRunSync()
  }

}

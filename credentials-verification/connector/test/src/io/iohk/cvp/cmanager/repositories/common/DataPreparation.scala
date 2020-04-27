package io.iohk.cvp.cmanager.repositories.common

import java.time.LocalDate
import java.util.UUID

import cats.effect.IO
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.{Credential, Issuer, IssuerGroup, Student}
import io.iohk.cvp.cmanager.repositories.{daos => cmanagerDaos}
import io.iohk.connector.repositories.{daos => connectorDaos}
import doobie.implicits._
import io.iohk.connector.model.{ParticipantInfo, ParticipantType}
import io.iohk.cvp.cmanager.models.requests.{CreateCredential, CreateStudent}
import io.iohk.cvp.models.ParticipantId

object DataPreparation {

  import cmanagerDaos._
  import connectorDaos._

  def createIssuer(name: String = "Issuer", tag: String = "")(implicit database: Transactor[IO]): Issuer = {
    val id = Issuer.Id(UUID.randomUUID())
    val did = s"did:geud:issuer-x$tag"
    sql"""
         |INSERT INTO issuers (issuer_id, name, did)
         |VALUES ($id, $name, $did)
         |""".stripMargin.update.run.transact(database).unsafeRunSync()

    // dirty hack to create a participant while creating an issuer, TODO: Merge the tables
    val participant = ParticipantInfo(ParticipantId(id.value), ParticipantType.Issuer, None, name, Option(did), None)
    sql"""
         |INSERT INTO participants (id, tpe, name, did)
         |VALUES (${participant.id}, ${participant.tpe}, ${participant.name}, ${participant.did})
       """.stripMargin.update.run.transact(database).unsafeRunSync()

    Issuer(id, Issuer.Name(name), did)
  }

  def createCredential(issuedBy: Issuer.Id, studentId: Student.Id, tag: String = "")(implicit
      database: Transactor[IO]
  ): Credential = {
    val request = CreateCredential(
      issuedBy = issuedBy,
      studentId = studentId,
      title = s"Major IN Applied Blockchain $tag".trim,
      enrollmentDate = LocalDate.now(),
      graduationDate = LocalDate.now().plusYears(5),
      groupName = s"Computer Science $tag".trim
    )

    CredentialsDAO.create(request).transact(database).unsafeRunSync()
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

    StudentsDAO.create(request, group.id).transact(database).unsafeRunSync()
  }

  def createIssuerGroup(issuer: Issuer.Id, name: IssuerGroup.Name)(implicit database: Transactor[IO]): IssuerGroup = {
    IssuerGroupsDAO.create(issuer, name).transact(database).unsafeRunSync()
  }
}

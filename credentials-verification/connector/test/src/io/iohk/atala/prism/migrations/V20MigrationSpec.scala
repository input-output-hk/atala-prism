package io.iohk.atala.prism.migrations

import java.util.UUID

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.crypto.ECPublicKey
import io.iohk.atala.prism.cmanager.models.{IssuerGroup, Student}
import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.console.models.{Contact, Institution}
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.repositories.PostgresMigrationSpec

object DataHelper {
  def createParticipant(
      tpe: ParticipantType,
      name: String,
      did: String,
      publicKey: Option[ECPublicKey],
      logo: Option[ParticipantLogo]
  )(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    sql"""INSERT INTO participants(id, tpe, did, public_key, name, logo) VALUES
          (${UUID.randomUUID()}, $tpe::PARTICIPANT_TYPE, $did, $publicKey, $name, $logo)
          RETURNING id"""
      .runUnique[ParticipantId]()
  }

  def createIssuer(id: ParticipantId)(implicit
      database: Transactor[IO]
  ): Unit = {
    val _ = sql"""INSERT INTO issuers(issuer_id) VALUES($id)"""
      .runUpdate()
  }

  def createGroup(issuer: ParticipantId, name: String)(implicit
      database: Transactor[IO]
  ): IssuerGroup = {
    val groupId = UUID.randomUUID()
    sql"""INSERT INTO issuer_groups (group_id,issuer_id,name)
         |VALUES ($groupId, ${issuer.uuid}, $name)
         |""".stripMargin.runUpdate()
    IssuerGroup(IssuerGroup.Id(groupId), IssuerGroup.Name(name), Institution.Id(issuer.uuid))
  }

  def createIssuer(name: String = "Issuer", logo: Option[ParticipantLogo] = None)(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    val pid = createParticipant(ParticipantType.Issuer, name, s"did:test:${name.toLowerCase}", None, logo)
    createIssuer(pid)
    pid
  }
}

class V20MigrationSpec extends PostgresMigrationSpec("V20") with BaseDAO {

  import DataHelper._

  test(
    beforeApply = {
      val issuerId = createIssuer("test-issuer")
      val group = createGroup(issuerId, "test-group")
      val subjectId = Contact.Id(UUID.randomUUID())
      val status: Student.ConnectionStatus = Student.ConnectionStatus.InvitationMissing
      sql"""
             |INSERT INTO issuer_subjects (subject_id, created_at, connection_status, group_id, subject_data)
             |VALUES($subjectId, NOW(), $status::STUDENT_CONNECTION_STATUS_TYPE, ${group.id}, jsonb_build_object('other_id', 'none'))""".stripMargin
        .runUpdate()
    }
  )
}

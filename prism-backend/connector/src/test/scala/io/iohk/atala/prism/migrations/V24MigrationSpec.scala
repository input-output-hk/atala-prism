package io.iohk.atala.prism.migrations

import java.util.UUID

import doobie.implicits.toSqlInterpolator
import doobie.postgres.implicits.pgEnumString
import doobie.util.invariant.InvalidEnum
import doobie.Meta
import enumeratum.{Enum, EnumEntry}
import enumeratum.EnumEntry.UpperSnakecase
import io.iohk.atala.prism.connector.model.{ConnectionStatus, ParticipantType}
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.repositories.PostgresMigrationSpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits

// Models from the legacy cstore, used only in this test
object LegacyModels {
  sealed trait IndividualConnectionStatus extends EnumEntry with UpperSnakecase

  object IndividualConnectionStatus extends Enum[IndividualConnectionStatus] {
    val values = findValues

    case object Created extends IndividualConnectionStatus
    case object Invited extends IndividualConnectionStatus
    case object Connected extends IndividualConnectionStatus
    case object Revoked extends IndividualConnectionStatus
  }
}

class V24MigrationSpec extends PostgresMigrationSpec("V24") with BaseDAO {

  import LegacyModels._

  private def insertIssuer(issuerId: UUID, name: String, did: String): Unit = {
    sql"""INSERT INTO participants(id, tpe, did, public_key, name, logo)
         |VALUES ($issuerId, ${ParticipantType.Issuer: ParticipantType}::PARTICIPANT_TYPE, $did, null, $name, '')
         |""".stripMargin.runUpdate()
  }

  private def insertVerifier(
      verifierId: UUID,
      name: String,
      did: String
  ): Unit = {
    sql"""INSERT INTO participants(id, tpe, did, public_key, name, logo)
         |VALUES ($verifierId, ${ParticipantType.Verifier: ParticipantType}::PARTICIPANT_TYPE, $did, null, $name, '')
         |""".stripMargin.runUpdate()
  }

  private def insertGroup(
      issuerId: UUID,
      groupId: UUID,
      groupName: String
  ): Unit = {
    sql"""INSERT INTO issuer_groups (group_id,issuer_id,name)
         |VALUES ($groupId, $issuerId, $groupName)
         |""".stripMargin.runUpdate()
  }

  private def insertSubject(subjectId: UUID, issuerId: UUID): Unit = {
    val externalId = UUID.randomUUID()
    sql"""INSERT INTO issuer_subjects (subject_id , external_id , created_at , connection_status , issuer_id , subject_data)
         |VALUES ($subjectId, $externalId, now(), 'INVITATION_MISSING',$issuerId,'{}')
         |""".stripMargin.runUpdate()
  }

  private def insertHolder(holderId: UUID, verifierId: UUID): Unit = {
    sql"""INSERT INTO verifier_holders (verifier_id, holder_id, holder_data, created_at)
         |VALUES ($verifierId, $holderId, '{}'::jsonb, now())
         |""".stripMargin.runUpdate()
  }

  private def assignGroup(subjectId: UUID, groupId: UUID): Unit = {
    sql"""INSERT INTO contacts_per_group (group_id, subject_id, added_at)
         |VALUES ($groupId, $subjectId, now())
         |""".stripMargin.runUpdate()
  }

  private def setSubjectStatus(
      subjectId: UUID,
      status: Student.ConnectionStatus
  ): Unit = {
    sql"""
         |UPDATE issuer_subjects
         |SET connection_status = ${status: Student.ConnectionStatus}::STUDENT_CONNECTION_STATUS_TYPE
         |WHERE subject_id = $subjectId
         |""".stripMargin.runUpdate()
  }

  private def contactStatusFor(contactId: UUID): ConnectionStatus = {
    implicit val contactConnectionStatusMeta: Meta[ConnectionStatus] =
      Meta[String].timap(ConnectionStatus.withNameInsensitive)(_.entryName)

    sql"""
         |SELECT connection_status
         |FROM contacts
         |WHERE contact_id = $contactId
         |""".stripMargin.runUnique[ConnectionStatus]()
  }

  private def setHolderStatus(
      holderId: UUID,
      status: IndividualConnectionStatus
  ): Unit = {
    implicit val pgPackageTypeMeta: Meta[IndividualConnectionStatus] =
      pgEnumString[IndividualConnectionStatus](
        "INDIVIDUAL_CONNECTION_STATUS_TYPE",
        a =>
          IndividualConnectionStatus
            .withNameOption(a)
            .getOrElse(throw InvalidEnum[IndividualConnectionStatus](a)),
        _.entryName
      )

    sql"""
         |UPDATE verifier_holders
         |SET connection_status = ${status: IndividualConnectionStatus}::INDIVIDUAL_CONNECTION_STATUS_TYPE
         |WHERE holder_id = $holderId
         |""".stripMargin.runUpdate()
  }

  private def assignedIssuerForSubject(subjectId: UUID): UUID = {
    sql"""SELECT created_by FROM contacts
         |WHERE contact_id = $subjectId
         |""".stripMargin.runUnique[UUID]()
  }

  private def subjectsInGroup(groupId: UUID): Seq[UUID] = {
    sql"""SELECT contact_id FROM contacts_per_group
         |WHERE group_id = $groupId
         |""".stripMargin.queryList[UUID]()
  }

  private def assignedVerifierForHolder(holderId: UUID): UUID = {
    sql"""SELECT created_by FROM contacts
         |WHERE contact_id = $holderId
         |""".stripMargin.runUnique[UUID]()
  }

  private def addCredential(
      credentialId: UUID,
      subjectId: UUID,
      issuerId: UUID,
      groupName: String
  ): Unit = {
    sql"""INSERT INTO credentials (credential_id, issuer_id, subject_id, credential_data, group_name, created_on)
         |VALUES ($credentialId, $issuerId, $subjectId, '{}'::jsonb, $groupName, now())
         |""".stripMargin.runUpdate()
  }

  private def credentialIdIssuedFor(subjectId: UUID): UUID = {
    sql"""SELECT credential_id FROM credentials
         |WHERE subject_id = $subjectId
         |""".stripMargin.runUnique[UUID]()
  }

  private val issuerId1 = UUID.randomUUID()
  private val issuerId2 = UUID.randomUUID()
  private val issuerId3 = UUID.randomUUID()
  private val issuerId4 = UUID.randomUUID()
  private val issuerId5 = UUID.randomUUID()
  private val issuerId6 = UUID.randomUUID()
  private val groupId1 = UUID.randomUUID()
  private val groupId2 = UUID.randomUUID()
  private val groupId3 = UUID.randomUUID()
  private val groupId4 = UUID.randomUUID()
  private val groupId5 = UUID.randomUUID()
  private val groupId6 = UUID.randomUUID()
  private val groupId7 = UUID.randomUUID()
  private val subjectId1 = UUID.randomUUID()
  private val subjectId2 = UUID.randomUUID()
  private val subjectId3 = UUID.randomUUID()
  private val subjectId4 = UUID.randomUUID()
  private val subjectId5 = UUID.randomUUID()
  private val subjectId6 = UUID.randomUUID()
  private val subjectId7 = UUID.randomUUID()
  private val subjectId8 = UUID.randomUUID()
  private val subjectId9 = UUID.randomUUID()
  private val subjectId10 = UUID.randomUUID()
  private val subjectId11 = UUID.randomUUID()
  private val credentialId1 = UUID.randomUUID()
  private val credentialId2 = UUID.randomUUID()
  private val credentialId3 = UUID.randomUUID()

  private val verifierId1 = UUID.randomUUID()
  private val verifierId2 = UUID.randomUUID()
  private val verifierId3 = UUID.randomUUID()
  private val holderId1 = UUID.randomUUID()
  private val holderId2 = UUID.randomUUID()
  private val holderId3 = UUID.randomUUID()
  private val holderId4 = UUID.randomUUID()
  private val holderId5 = UUID.randomUUID()

  test(
    beforeApply = {
      // Issuer 1 has 2 groups with 2 subjects per group
      insertIssuer(
        issuerId1,
        "issuer 1",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertGroup(issuerId1, groupId1, "Group 1")
      insertSubject(subjectId1, issuerId1)
      assignGroup(subjectId1, groupId1)
      insertSubject(subjectId2, issuerId1)
      assignGroup(subjectId2, groupId1)
      // we issue a credential to the first subject
      addCredential(credentialId1, subjectId1, issuerId1, "Group 1")
      insertGroup(issuerId1, groupId2, "Group 2")
      insertSubject(subjectId3, issuerId1)
      assignGroup(subjectId3, groupId2)
      insertSubject(subjectId4, issuerId1)
      assignGroup(subjectId4, groupId2)
      // Issuer 2 has 1 group, also with 2 subjects
      insertIssuer(
        issuerId2,
        "issuer 2",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertGroup(issuerId2, groupId3, "Group 3")
      insertSubject(subjectId5, issuerId2)
      assignGroup(subjectId5, groupId3)
      insertSubject(subjectId6, issuerId2)
      assignGroup(subjectId6, groupId3)
      // we issue a credential to both subjects in group 3
      addCredential(credentialId2, subjectId5, issuerId2, "Group 3")
      addCredential(credentialId3, subjectId6, issuerId2, "Group 3")
      // Issuer 3 has 2 groups, but each group has a unique subject
      insertIssuer(
        issuerId3,
        "issuer 3",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertGroup(issuerId3, groupId4, "Group 4")
      insertSubject(subjectId7, issuerId3)
      assignGroup(subjectId7, groupId4)
      insertGroup(issuerId3, groupId5, "Group 5")
      insertSubject(subjectId8, issuerId3)
      assignGroup(subjectId8, groupId5)
      // Issuer 4 has 1 group, which is empty
      insertIssuer(
        issuerId4,
        "issuer 4",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertGroup(issuerId4, groupId6, "Group 6")
      // Issuer 5 has 1 group, which is empty and one subject attached
      insertIssuer(
        issuerId5,
        "issuer 5",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertGroup(issuerId5, groupId7, "Group 7")
      insertSubject(subjectId9, issuerId5)
      // Issuer 6 has no groups and two subject attached
      insertIssuer(
        issuerId6,
        "issuer 6",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertSubject(subjectId10, issuerId6)
      insertSubject(subjectId11, issuerId6)

      // We test the 4 connection statuses
      setSubjectStatus(subjectId2, Student.ConnectionStatus.InvitationMissing)
      setSubjectStatus(subjectId3, Student.ConnectionStatus.ConnectionMissing)
      setSubjectStatus(subjectId4, Student.ConnectionStatus.ConnectionAccepted)
      setSubjectStatus(subjectId5, Student.ConnectionStatus.ConnectionRevoked)

      // Verifier 1 has 2 holders
      insertVerifier(
        verifierId1,
        "Verifier 1",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertHolder(holderId1, verifierId1)
      insertHolder(holderId2, verifierId1)
      // Verifier 2 has no holders assigned
      insertVerifier(
        verifierId2,
        "Verifier 2",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      // Verifier 3 has 3 holders assigned
      insertVerifier(
        verifierId3,
        "Verifier 3",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertHolder(holderId3, verifierId3)
      insertHolder(holderId4, verifierId3)
      insertHolder(holderId5, verifierId3)

      // We set the different connection statuses to the holders
      setHolderStatus(holderId2, IndividualConnectionStatus.Created)
      setHolderStatus(holderId3, IndividualConnectionStatus.Invited)
      setHolderStatus(holderId4, IndividualConnectionStatus.Connected)
      setHolderStatus(holderId5, IndividualConnectionStatus.Revoked)
    },
    afterApplied = {
      assignedIssuerForSubject(subjectId1) mustBe issuerId1
      assignedIssuerForSubject(subjectId2) mustBe issuerId1
      assignedIssuerForSubject(subjectId3) mustBe issuerId1
      assignedIssuerForSubject(subjectId4) mustBe issuerId1
      assignedIssuerForSubject(subjectId5) mustBe issuerId2
      assignedIssuerForSubject(subjectId6) mustBe issuerId2
      assignedIssuerForSubject(subjectId7) mustBe issuerId3
      assignedIssuerForSubject(subjectId8) mustBe issuerId3
      assignedIssuerForSubject(subjectId9) mustBe issuerId5
      assignedIssuerForSubject(subjectId10) mustBe issuerId6
      assignedIssuerForSubject(subjectId11) mustBe issuerId6

      subjectsInGroup(groupId1) must contain theSameElementsAs Seq(
        subjectId1,
        subjectId2
      )
      subjectsInGroup(groupId2) must contain theSameElementsAs Seq(
        subjectId3,
        subjectId4
      )
      subjectsInGroup(groupId3) must contain theSameElementsAs Seq(
        subjectId5,
        subjectId6
      )
      subjectsInGroup(groupId4) must contain theSameElementsAs Seq(subjectId7)
      subjectsInGroup(groupId5) must contain theSameElementsAs Seq(subjectId8)
      subjectsInGroup(groupId6) must be(empty)
      subjectsInGroup(groupId7) must be(empty)

      val allRows = sql"SELECT * FROM contacts_per_group".queryList[UUID]()
      allRows.size mustBe 8

      contactStatusFor(subjectId1) mustBe ConnectionStatus.InvitationMissing
      contactStatusFor(subjectId2) mustBe ConnectionStatus.InvitationMissing
      contactStatusFor(subjectId3) mustBe ConnectionStatus.ConnectionMissing
      contactStatusFor(subjectId4) mustBe ConnectionStatus.ConnectionAccepted
      contactStatusFor(subjectId5) mustBe ConnectionStatus.ConnectionRevoked

      credentialIdIssuedFor(subjectId1) mustBe credentialId1
      credentialIdIssuedFor(subjectId5) mustBe credentialId2
      credentialIdIssuedFor(subjectId6) mustBe credentialId3

      assignedVerifierForHolder(holderId1) mustBe verifierId1
      assignedVerifierForHolder(holderId2) mustBe verifierId1
      assignedVerifierForHolder(holderId3) mustBe verifierId3
      assignedVerifierForHolder(holderId4) mustBe verifierId3
      assignedVerifierForHolder(holderId5) mustBe verifierId3

      contactStatusFor(holderId1) mustBe ConnectionStatus.InvitationMissing
      contactStatusFor(holderId2) mustBe ConnectionStatus.InvitationMissing
      contactStatusFor(holderId3) mustBe ConnectionStatus.ConnectionMissing
      contactStatusFor(holderId4) mustBe ConnectionStatus.ConnectionAccepted
      contactStatusFor(holderId5) mustBe ConnectionStatus.ConnectionRevoked
    }
  )
}

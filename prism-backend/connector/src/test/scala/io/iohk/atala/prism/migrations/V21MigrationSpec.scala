package io.iohk.atala.prism.migrations

import java.util.UUID

import doobie.implicits._
import io.iohk.atala.prism.connector.model.ParticipantType
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.repositories.PostgresMigrationSpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits

class V21MigrationSpec extends PostgresMigrationSpec("V21") with BaseDAO {

  private def insertIssuer(issuerId: UUID, name: String, did: String): Unit = {
    sql"""INSERT INTO participants(id, tpe, did, public_key, name, logo)
         |VALUES ($issuerId, ${ParticipantType.Issuer: ParticipantType}::PARTICIPANT_TYPE, $did, null, $name, '')
         |""".stripMargin.runUpdate()
    sql"""INSERT INTO issuers (issuer_id) VALUES ($issuerId)""".runUpdate()
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

  private def insertSubject(subjectId: UUID, groupId: UUID): Unit = {
    val externalId = UUID.randomUUID()
    sql"""INSERT INTO issuer_subjects (subject_id , external_id , created_at , connection_status , group_id , subject_data)
       |VALUES ($subjectId, $externalId, now(), 'CONNECTION_MISSING',$groupId,'{}')
       |""".stripMargin.runUpdate()
  }

  private def assignedIssuerForSubject(subjectId: UUID): UUID = {
    sql"""SELECT issuer_id FROM issuer_subjects
         |WHERE subject_id = $subjectId
         |""".stripMargin.runUnique[UUID]()
  }

  private def subjectsInGroup(groupId: UUID): Seq[UUID] = {
    sql"""SELECT subject_id FROM contacts_per_group
         |WHERE group_id = $groupId
         |""".stripMargin.queryList[UUID]()
  }

  private val issuerId1 = UUID.randomUUID()
  private val issuerId2 = UUID.randomUUID()
  private val issuerId3 = UUID.randomUUID()
  private val issuerId4 = UUID.randomUUID()
  private val groupId1 = UUID.randomUUID()
  private val groupId2 = UUID.randomUUID()
  private val groupId3 = UUID.randomUUID()
  private val groupId4 = UUID.randomUUID()
  private val groupId5 = UUID.randomUUID()
  private val groupId6 = UUID.randomUUID()
  private val subjectId1 = UUID.randomUUID()
  private val subjectId2 = UUID.randomUUID()
  private val subjectId3 = UUID.randomUUID()
  private val subjectId4 = UUID.randomUUID()
  private val subjectId5 = UUID.randomUUID()
  private val subjectId6 = UUID.randomUUID()
  private val subjectId7 = UUID.randomUUID()
  private val subjectId8 = UUID.randomUUID()

  test(
    beforeApply = {
      // Issuer 1 has 2 groups with 2 subjects per group
      insertIssuer(
        issuerId1,
        "issuer 1",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertGroup(issuerId1, groupId1, "Group 1")
      insertSubject(subjectId1, groupId1)
      insertSubject(subjectId2, groupId1)
      insertGroup(issuerId1, groupId2, "Group 2")
      insertSubject(subjectId3, groupId2)
      insertSubject(subjectId4, groupId2)
      // Issuer 2 has 1 group, also with 2 subjects
      insertIssuer(
        issuerId2,
        "issuer 2",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertGroup(issuerId2, groupId3, "Group 3")
      insertSubject(subjectId5, groupId3)
      insertSubject(subjectId6, groupId3)
      // Issuer 3 has 2 groups, but each group has a unique subject
      insertIssuer(
        issuerId3,
        "issuer 3",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertGroup(issuerId3, groupId4, "Group 4")
      insertSubject(subjectId7, groupId4)
      insertGroup(issuerId3, groupId5, "Group 5")
      insertSubject(subjectId8, groupId5)
      // Issuer 4 has 1 group, which is empty
      insertIssuer(
        issuerId4,
        "issuer 4",
        "did:prism:asdasdasdasdasdaasdasdasdasdasda"
      )
      insertGroup(issuerId4, groupId6, "Group 6")
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

      val allRows = sql"SELECT * FROM contacts_per_group".queryList[UUID]()
      allRows.size mustBe 8
    }
  )
}

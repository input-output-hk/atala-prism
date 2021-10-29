package io.iohk.atala.prism.migrations

import java.util.UUID
import doobie.implicits._
import io.iohk.atala.prism.connector.model.ParticipantType
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.repositories.PostgresMigrationSpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits

class V40MigrationSpec extends PostgresMigrationSpec("V40") with BaseDAO {
  private val problemDid1 = "did:prism:asdasdasdasdasdaasdasdasdasdasda1"
  private val problemDid2 = "did:prism:asdasdasdasdasdaasdasdasdasdasda2"
  private val issuer1Did = "did:prism:issuer1"
  private val issuer2Did = "did:prism:issuer2"

  private def getActualState: List[String] =
    sql"SELECT did FROM participants".queryList[String]().toList

  private def insertParticipants(
      issuerId: UUID,
      name: String,
      did: String
  ): Unit =
    sql"""INSERT INTO participants(id, tpe, did, public_key, name, logo)
         |VALUES ($issuerId, ${ParticipantType.Issuer: ParticipantType}::PARTICIPANT_TYPE, $did, null, $name, '')
         |""".stripMargin.runUpdate()

  test(
    beforeApply = {
      insertParticipants(UUID.randomUUID(), "someName1", issuer1Did)
      insertParticipants(UUID.randomUUID(), "someName2", issuer2Did)
      insertParticipants(UUID.randomUUID(), "someName3", problemDid1)
      insertParticipants(UUID.randomUUID(), "someName3", problemDid1)
      insertParticipants(UUID.randomUUID(), "someName4", problemDid2)
      insertParticipants(UUID.randomUUID(), "someName4", problemDid2)
      insertParticipants(UUID.randomUUID(), "someName4", problemDid2)
      val stateBeforeMigration = getActualState
      // 7 from initial, 4 originals new, 3 duplicates
      stateBeforeMigration.size mustBe 14
      stateBeforeMigration.count(_ == problemDid1) mustBe 2
      stateBeforeMigration.count(_ == problemDid2) mustBe 3
    },
    afterApplied = {
      val stateAfterMigration = getActualState
      // - 3 duplicates
      stateAfterMigration.size mustBe 11
      stateAfterMigration.count(_ == problemDid1) mustBe 1
      stateAfterMigration.count(_ == problemDid2) mustBe 1
    }
  )

}

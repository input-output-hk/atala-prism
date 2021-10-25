package io.iohk.atala.prism.management.console.migrations

import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.circe.Json
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.repositories.PostgresMigrationSpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits

import java.time.Instant
import java.util.UUID

class V15MigrationSpec extends PostgresMigrationSpec("V15") with BaseDAO {

  val credentialId1: UUID = UUID.randomUUID()
  val credentialId2: UUID = UUID.randomUUID()
  val createdOn: Instant = Instant.now
  val createdAt: Instant = Instant.now
  val issuerId: UUID = UUID.randomUUID()
  val contactId: UUID = UUID.randomUUID()
  val credentialTypeId: UUID = UUID.randomUUID()
  val credentialIssuanceId: UUID = UUID.randomUUID()
  val credentialIssuanceContactId: UUID = UUID.randomUUID()
  val jsonData: Json = Json.fromString("{\"data\": \"some data\"}")

  private def prepare(): Unit = {
    createIssuer()
    createContact()
    createCredentialType()
    createCredentialIssuance()
    createCredentialIssuanceContact()
  }

  private def createIssuer(): Unit = {
    sql"""INSERT INTO participants (participant_id, name, did, created_at)
         |VALUES ($issuerId, 'The Issuer', 'did:prism:theissuer', ${Instant.now})""".stripMargin
      .runUpdate()
  }

  private def createContact(): Unit = {
    sql"""INSERT INTO contacts (contact_id, external_id, created_by, contact_data, created_at, name, connection_token)
         |VALUES ($contactId, 'someid', $issuerId, $jsonData, ${Instant.now}, 'The Contact', 'JRRTolkien')""".stripMargin
      .runUpdate()
  }

  private def createCredentialType(): Unit = {
    sql"""INSERT INTO credential_types (credential_type_id, name, institution_id, state, template, created_at)
         |VALUES ($credentialTypeId, 'The Credential Type', $issuerId, 'DRAFT', 'a template', ${Instant.now})""".stripMargin
      .runUpdate()
  }

  private def createCredentialIssuance(): Unit = {
    sql"""INSERT INTO credential_issuances (credential_issuance_id, name, created_by, created_at, credential_type_id)
         |VALUES ($credentialIssuanceId, 'The Credential Issuance', $issuerId, ${Instant.now}, $credentialTypeId)""".stripMargin
      .runUpdate()
  }

  private def createCredentialIssuanceContact(): Unit = {
    sql"""INSERT INTO credential_issuance_contacts (credential_issuance_contact_id, contact_id, credential_data, credential_issuance_id)
         |VALUES ($credentialIssuanceContactId, $contactId, $jsonData, $credentialIssuanceId)""".stripMargin
      .runUpdate()
  }

  private def insertDraftCredentialWithCreatedOn(
      credentialId: UUID,
      data: Json,
      createdAt: Instant
  ): Unit = {
    sql"""INSERT INTO draft_credentials (credential_id, issuer_id, contact_id, credential_data,
         |    created_on, credential_issuance_contact_id, credential_type_id)
         |  VALUES ($credentialId, $issuerId, $contactId, $data,
         |    $createdAt, $credentialIssuanceContactId, $credentialTypeId)""".stripMargin
      .runUpdate()
  }

  private def insertDraftCredentialWithCreatedAt(
      credentialId: UUID,
      data: Json,
      createdAt: Instant
  ): Unit = {
    sql"""INSERT INTO draft_credentials (credential_id, issuer_id, contact_id, credential_data,
         |    created_at, credential_issuance_contact_id, credential_type_id)
         |  VALUES ($credentialId, $issuerId, $contactId, $data,
         |    $createdAt, $credentialIssuanceContactId, $credentialTypeId)""".stripMargin
      .runUpdate()
  }

  private def getLastCreatedAt(credentialId: UUID): Instant =
    sql"SELECT created_at FROM draft_credentials WHERE credential_id = $credentialId"
      .runUnique[Instant]()

  private def getLastCreatedOn(credentialId: UUID): Instant =
    sql"SELECT created_on FROM draft_credentials WHERE credential_id = $credentialId"
      .runUnique[Instant]()

  test(
    beforeApply = {
      prepare()

      insertDraftCredentialWithCreatedOn(
        credentialId1,
        jsonData,
        createdOn
      )

      getLastCreatedOn(credentialId1) mustBe createdOn
    },
    afterApplied = {
      insertDraftCredentialWithCreatedAt(
        credentialId2,
        jsonData,
        createdAt
      )

      getLastCreatedAt(credentialId1) mustBe createdOn
      getLastCreatedAt(credentialId2) mustBe createdAt
    }
  )

}

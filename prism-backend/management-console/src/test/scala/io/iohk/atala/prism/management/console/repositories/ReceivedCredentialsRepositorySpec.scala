package io.iohk.atala.prism.management.console.repositories

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.models.{Contact, CredentialExternalId, ParticipantId, ParticipantLogo}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationInt

class ReceivedCredentialsRepositorySpec extends PostgresRepositorySpec {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)

  lazy val receivedCredentialsRepository = new ReceivedCredentialsRepository(database)
  lazy val participantsRepository = new ParticipantsRepository(database)

  lazy val verifierId = ParticipantId(UUID.fromString("af45a4da-65b8-473e-aadc-aa6b346250a3"))

  override def beforeEach(): Unit = {
    super.beforeEach()

    participantsRepository
      .create(
        CreateParticipantRequest(
          verifierId,
          "Verifier",
          DID.buildPrismDID("test"),
          ParticipantLogo(Vector())
        )
      )
      .value
      .futureValue
    ()
  }

  def create(
      contactId: Contact.Id,
      encodedSignedCredential: String,
      credentialExternalId: CredentialExternalId
  ): Unit = {
    receivedCredentialsRepository
      .createReceivedCredential(
        ReceivedSignedCredentialData(
          contactId,
          encodedSignedCredential,
          credentialExternalId
        )
      )
      .value
      .futureValue
    ()
  }

  "ReceivedCredentialsRepository" should {
    "be able to create a new retrievable credential" in {
      val contactId = DataPreparation.createContact(verifierId, "Individual", None, "").contactId

      val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()

      create(contactId, encodedSignedCredential, mockCredentialExternalId)

      val result =
        receivedCredentialsRepository.getCredentialsFor(verifierId, contactId).value.futureValue.toOption.value
      result.size must be(1)
      val resultCredential = result.head

      resultCredential.encodedSignedCredential must be(encodedSignedCredential)
      resultCredential.individualId must be(contactId)
      assert(
        resultCredential.receivedAt.isBefore(Instant.now()) &&
          resultCredential.receivedAt.isAfter(Instant.now().minus(2, ChronoUnit.MINUTES))
      )
    }

    "be able to fetch latest credentials for a verifier" in {
      val contactId = DataPreparation.createContact(verifierId, "Individual", None, "").contactId

      val encodedSignedCredential1 = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId1 = CredentialExternalId.random()

      create(contactId, encodedSignedCredential1, mockCredentialExternalId1)

      val encodedSignedCredential2 = "b4cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId2 = CredentialExternalId.random()

      create(contactId, encodedSignedCredential2, mockCredentialExternalId2)

      val result =
        receivedCredentialsRepository.getLatestCredentialExternalId(verifierId).value.futureValue.toOption.value.value

      result must be(mockCredentialExternalId2)
    }
  }
}

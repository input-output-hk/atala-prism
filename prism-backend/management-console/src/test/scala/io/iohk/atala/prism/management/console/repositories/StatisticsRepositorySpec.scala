package io.iohk.atala.prism.management.console.repositories

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models.{
  GenericCredential,
  InstitutionGroup,
  ParticipantId,
  PublishCredential,
  TimeInterval
}
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}
import org.scalatest.OptionValues._

import java.time.Instant

class StatisticsRepositorySpec extends AtalaWithPostgresSpec {
  lazy val repository = new StatisticsRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  private def publishCredential(issuerId: ParticipantId, credential: GenericCredential): Either[Nothing, Int] = {
    credentialsRepository
      .storePublicationData(
        issuerId,
        PublishCredential(
          credential.credentialId,
          SHA256Digest.compute("test".getBytes),
          "mockNodeCredentialId",
          "mockEncodedSignedCredential",
          TransactionInfo(
            TransactionId.from("3d488d9381b09954b5a9606b365ab0aaeca6aa750bdba79436e416ad6702226a").value,
            Ledger.InMemory,
            None
          )
        )
      )
      .value
      .futureValue
  }

  "query" should {
    "work" in {
      val issuerId = createParticipant("Issuer-1")
      createInstitutionGroup(issuerId, InstitutionGroup.Name("Grp 1"))
      createContact(issuerId, "no-invite", None)
      createContact(issuerId, "pending-connection-1", None)
      createContact(issuerId, "pending-connection-2", None)
      val contact3 = createContact(issuerId, "connected", None)

      // credentials
      val credential1 = createGenericCredential(issuerId, contact3.contactId)
      createGenericCredential(issuerId, contact3.contactId)
      credentialsRepository
        .storePublicationData(
          issuerId,
          PublishCredential(
            credential1.credentialId,
            SHA256Digest.compute("test".getBytes),
            "mockNodeCredentialId",
            "mockEncodedSignedCredential",
            TransactionInfo(
              TransactionId.from("3d488d9381b09954b5a9606b365ab0aaeca6aa750bdba79436e416ad6702226a").value,
              Ledger.InMemory,
              None
            )
          )
        )
        .value
        .futureValue

      val result = repository.query(issuerId, None).value.futureValue.toOption.value
      result.numberOfContacts must be(4)

      result.numberOfGroups must be(1)
      result.numberOfCredentials must be(2)
      result.numberOfCredentialsPublished must be(1)
      result.numberOfCredentialsInDraft must be(1)
      result.numberOfCredentialsReceived must be(0)
    }

    "support time interval" in {
      val issuerId = createParticipant("Issuer-1")
      createInstitutionGroup(issuerId, InstitutionGroup.Name("Grp 1"))
      val contact1 = createContact(issuerId, "Contact 1")
      createGenericCredential(issuerId, contact1.contactId)
      val credential1 = createGenericCredential(issuerId, contact1.contactId)
      publishCredential(issuerId, credential1)

      Thread.sleep(10) // sleep to add some time padding for the inspected interval

      val start = Instant.now()
      createInstitutionGroup(issuerId, InstitutionGroup.Name("Grp 2"))
      val contact2 = createContact(issuerId, "Contact 2", None)
      createGenericCredential(issuerId, contact2.contactId)
      val credential2 = createGenericCredential(issuerId, contact2.contactId)
      publishCredential(issuerId, credential2)
      val end = Instant.now()

      Thread.sleep(10) // sleep to add some time padding for the inspected interval

      createInstitutionGroup(issuerId, InstitutionGroup.Name("Grp 3"))
      val contact3 = createContact(issuerId, "Contact 3", None)
      createGenericCredential(issuerId, contact3.contactId)
      val credential3 = createGenericCredential(issuerId, contact3.contactId)
      publishCredential(issuerId, credential3)

      val result = repository.query(issuerId, Some(TimeInterval(start, end))).value.futureValue.toOption.value
      result.numberOfContacts must be(1)

      result.numberOfGroups must be(1)
      result.numberOfCredentials must be(2)
      result.numberOfCredentialsPublished must be(1)
      result.numberOfCredentialsInDraft must be(1)
      result.numberOfCredentialsReceived must be(0)
    }
  }
}

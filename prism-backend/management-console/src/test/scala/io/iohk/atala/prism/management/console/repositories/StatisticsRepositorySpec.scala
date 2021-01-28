package io.iohk.atala.prism.management.console.repositories

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models.{InstitutionGroup, PublishCredential}
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}
import org.scalatest.OptionValues._

class StatisticsRepositorySpec extends AtalaWithPostgresSpec {
  lazy val repository = new StatisticsRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

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

      val result = repository.query(issuerId).value.futureValue.toOption.value
      result.numberOfContacts must be(4)

      result.numberOfGroups must be(1)
      result.numberOfCredentials must be(2)
      result.numberOfCredentialsPublished must be(1)
      result.numberOfCredentialsInDraft must be(1)
      result.numberOfCredentialsReceived must be(0)
    }
  }
}

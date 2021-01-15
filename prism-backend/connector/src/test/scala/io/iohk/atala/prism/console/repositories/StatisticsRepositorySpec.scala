package io.iohk.atala.prism.console.repositories

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.console.DataPreparation._
import io.iohk.atala.prism.console.models.{IssuerGroup, PublishCredential}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}
import org.scalatest.OptionValues._

class StatisticsRepositorySpec extends AtalaWithPostgresSpec {
  lazy val repository = new StatisticsRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  "query" should {
    "work" in {
      val issuerId = createIssuer("Issuer-1", "a")
      createIssuerGroup(issuerId, IssuerGroup.Name("Grp 1"))
      createContact(issuerId, "no-invite", None, "")
      val contact1 = createContact(issuerId, "pending-connection-1", None, "")
      val contact2 = createContact(issuerId, "pending-connection-2", None, "")
      val contact3 = createContact(issuerId, "connected", None, "")
      generateConnectionToken(issuerId, contact1.contactId)
      generateConnectionToken(issuerId, contact2.contactId)
      val token3 = generateConnectionToken(issuerId, contact3.contactId)
      acceptConnection(issuerId, token3)

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
      result.numberOfContactsPendingConnection must be(2)
      result.numberOfContactsConnected must be(1)

      result.numberOfGroups must be(1)
      result.numberOfCredentials must be(2)
      result.numberOfCredentialsPublished must be(1)
      result.numberOfCredentialsInDraft must be(1)
      // TODO: enable once receiving credentials is fixed
//      result.numberOfCredentialsReceived must be(0)
    }
  }
}

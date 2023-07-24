package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models.{InstitutionGroup, TimeInterval}
import io.iohk.atala.prism.utils.IOUtils._
import tofu.logging.Logs

import java.time.Instant

class StatisticsRepositorySpec extends AtalaWithPostgresSpec {

  val logs: Logs[IO, IO] = Logs.sync[IO, IO]

  lazy val repository = StatisticsRepository.unsafe(database, logs)
  lazy val credentialsRepository = CredentialsRepository.unsafe(database, logs)

  "query" should {
    "work" in {
      val issuerId = createParticipant("Issuer-1")
      createInstitutionGroup(issuerId, InstitutionGroup.Name("Grp 1"))
      createContact(issuerId, "no-invite", None)
      createContact(issuerId, "pending-connection-1", None)
      createContact(issuerId, "pending-connection-2", None)
      val contact3 = createContact(issuerId, "connected", None)

      // credentials
      val credential1 =
        createGenericCredential(issuerId, contact3.contactId, "A")
      createGenericCredential(issuerId, contact3.contactId, "B")
      publishCredential(issuerId, credential1)

      val result = repository.query(issuerId, None).unsafeRunSync()
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
      createGenericCredential(issuerId, contact1.contactId, "A")
      val credential1 =
        createGenericCredential(issuerId, contact1.contactId, "B")
      publishCredential(issuerId, credential1)

      Thread.sleep(
        50
      ) // sleep to add some time padding for the inspected interval
      val start = Instant.now()
      Thread.sleep(
        50
      ) // sleep to mitigate the time difference between database and the node

      createInstitutionGroup(issuerId, InstitutionGroup.Name("Grp 2"))
      val contact2 = createContact(issuerId, "Contact 2", None)
      createGenericCredential(issuerId, contact2.contactId, "C")
      val credential2 =
        createGenericCredential(issuerId, contact2.contactId, "D")
      publishCredential(issuerId, credential2)

      Thread.sleep(
        50
      ) // sleep to mitigate the time difference between database and the node
      val end = Instant.now()
      Thread.sleep(
        50
      ) // sleep to add some time padding for the inspected interval

      createInstitutionGroup(issuerId, InstitutionGroup.Name("Grp 3"))
      val contact3 = createContact(issuerId, "Contact 3", None)
      createGenericCredential(issuerId, contact3.contactId, "E")
      val credential3 =
        createGenericCredential(issuerId, contact3.contactId, "F")
      publishCredential(issuerId, credential3)

      val result = repository
        .query(issuerId, Some(TimeInterval(start, end)))
        .unsafeRunSync()
      result.numberOfContacts must be(1)

      result.numberOfGroups must be(1)
      result.numberOfCredentials must be(2)
      result.numberOfCredentialsPublished must be(1)
      result.numberOfCredentialsInDraft must be(1)
      result.numberOfCredentialsReceived must be(0)
    }
  }
}

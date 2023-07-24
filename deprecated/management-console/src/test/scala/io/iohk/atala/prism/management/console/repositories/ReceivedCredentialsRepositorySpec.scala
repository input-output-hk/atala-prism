package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.models.{Contact, CredentialExternalId, ParticipantId, ParticipantLogo}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.utils.IOUtils._
import tofu.logging.Logs

import java.time.Instant
import java.time.temporal.ChronoUnit
import io.iohk.atala.prism.crypto.MerkleInclusionProof

class ReceivedCredentialsRepositorySpec extends AtalaWithPostgresSpec {
  val logs: Logs[IO, IO] = Logs.sync[IO, IO]
  lazy val receivedCredentialsRepository =
    ReceivedCredentialsRepository.unsafe(database, logs)
  lazy val participantsRepository =
    ParticipantsRepository.unsafe(database, logs)

  lazy val verifierId =
    ParticipantId.unsafeFrom("af45a4da-65b8-473e-aadc-aa6b346250a3")

  override def beforeEach(): Unit = {
    super.beforeEach()

    participantsRepository
      .create(
        CreateParticipantRequest(
          verifierId,
          "Verifier",
          DataPreparation.newDID(),
          ParticipantLogo(Vector())
        )
      )
      .unsafeRunSync()
    ()
  }

  def create(
      contactId: Contact.Id,
      encodedSignedCredential: String,
      credentialExternalId: CredentialExternalId,
      batchInclusionProof: Option[MerkleInclusionProof]
  ): Unit = {
    receivedCredentialsRepository
      .createReceivedCredential(
        ReceivedSignedCredentialData(
          contactId,
          encodedSignedCredential,
          credentialExternalId,
          batchInclusionProof
        )
      )
      .unsafeRunSync()
    ()
  }

  "ReceivedCredentialsRepository" should {
    "be able to create a new retrievable credential" in {
      val contactId =
        DataPreparation.createContact(verifierId, "Individual", None).contactId

      val encodedSignedCredential =
        "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()

      create(contactId, encodedSignedCredential, mockCredentialExternalId, None)

      val result =
        receivedCredentialsRepository
          .getCredentialsFor(verifierId, Some(contactId))
          .unsafeRunSync()
      result.size must be(1)
      val resultCredential = result.head

      resultCredential.encodedSignedCredential must be(encodedSignedCredential)
      resultCredential.individualId must be(contactId)
      assert(
        resultCredential.receivedAt.isBefore(Instant.now()) &&
          resultCredential.receivedAt.isAfter(
            Instant.now().minus(2, ChronoUnit.MINUTES)
          )
      )
    }

    "be able to fetch latest credentials for a verifier" in {
      val contactId =
        DataPreparation.createContact(verifierId, "Individual", None).contactId

      val encodedSignedCredential1 =
        "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId1 = CredentialExternalId.random()

      create(contactId, encodedSignedCredential1, mockCredentialExternalId1, None)

      // Add time padding to make sure that the second credential is created strictly after the first one
      Thread.sleep(10)

      val encodedSignedCredential2 =
        "b4cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId2 = CredentialExternalId.random()

      create(contactId, encodedSignedCredential2, mockCredentialExternalId2, None)

      val result =
        receivedCredentialsRepository
          .getLatestCredentialExternalId(verifierId)
          .unsafeRunSync()

      result must be(Some(mockCredentialExternalId2))
    }

    "be return the batchInclusionProof if one is set (ATL-220)" in {
      val contactId =
        DataPreparation.createContact(verifierId, "Individual", None).contactId

      val encodedSignedCredential1 =
        "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId1 = CredentialExternalId.random()
      val merkleInclusionProof = MerkleInclusionProof.decode(
        """{"hash":"7d25e48be1c6475429bd33adbd5b7657340f264e62c2bf9b25ea478d9d3a2566","index":0,"siblings":[]}"""
      )

      create(contactId, encodedSignedCredential1, mockCredentialExternalId1, Some(merkleInclusionProof))

      // Add time padding to make sure that the second credential is created strictly after the first one
      Thread.sleep(10)

      val result =
        receivedCredentialsRepository
          .getCredentialsFor(verifierId, Some(contactId))
          .unsafeRunSync()

      result.head.batchInclusionProof must be(Some(merkleInclusionProof))

    }

  }
}

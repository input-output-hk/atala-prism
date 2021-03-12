package io.iohk.atala.prism.management.console

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.management.console.repositories.daos.{
  ContactsDAO,
  CredentialTypeDao,
  CredentialsDAO,
  InstitutionGroupsDAO,
  ParticipantsDAO,
  ReceivedCredentialsDAO
}
import io.iohk.atala.prism.models.{ConnectionToken, Ledger, TransactionId, TransactionInfo}
import org.scalatest.OptionValues._
import io.scalaland.chimney.dsl._
import io.iohk.atala.prism.protos.console_models
import io.iohk.atala.prism.protos.console_models.GenerateConnectionTokensRequestMetadata

import java.time.{Instant, LocalDate}
import scala.util.Random

object DataPreparation {
  def createParticipant(name: String)(implicit database: Transactor[IO]): ParticipantId = {
    createParticipant(name, newDID())
  }

  def createParticipant(
      name: String,
      did: DID
  )(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    val id = ParticipantId.random()
    val participant = ParticipantInfo(id, name, did, None)
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()
    id
  }

  val generateConnectionTokenRequestMetadata: ConnectorAuthenticatedRequestMetadata =
    ConnectorAuthenticatedRequestMetadata(
      did = newDID().toString,
      didKeyId = "didKeyId",
      didSignature = "didSignature",
      requestNonce = "requestNonce"
    )

  val generateConnectionTokenRequestProto: GenerateConnectionTokensRequestMetadata =
    generateConnectionTokenRequestMetadata.transformInto[console_models.GenerateConnectionTokensRequestMetadata]

  def createInstitutionGroup(institutionId: ParticipantId, name: InstitutionGroup.Name)(implicit
      database: Transactor[IO]
  ): InstitutionGroup = {
    InstitutionGroupsDAO.create(institutionId, name).transact(database).unsafeRunSync()
  }

  def createContact(
      institutionId: ParticipantId,
      name: String = s"name-${Random.nextInt(100)}",
      groupName: Option[InstitutionGroup.Name] = None,
      createdAt: Option[Instant] = None,
      externalId: Contact.ExternalId = Contact.ExternalId.random()
  )(implicit
      database: Transactor[IO]
  ): Contact = {
    val request = CreateContact(
      data = Json.obj(
        "email" -> "donthaveone@here.com".asJson,
        "admissionDate" -> LocalDate.now().asJson
      ),
      externalId = externalId,
      name = name,
      generateConnectionTokenRequestMetadata = generateConnectionTokenRequestMetadata
    )

    groupName match {
      case None =>
        ContactsDAO
          .createContact(institutionId, request, createdAt.getOrElse(Instant.now()), ConnectionToken("connectionToken"))
          .transact(database)
          .unsafeRunSync()
      case Some(name) =>
        val group = InstitutionGroupsDAO
          .find(institutionId, name)
          .transact(database)
          .unsafeRunSync()
          .getOrElse(throw new RuntimeException(s"Group $name does not exist"))

        val query = for {
          contact <- ContactsDAO.createContact(
            institutionId,
            request,
            createdAt.getOrElse(Instant.now()),
            ConnectionToken("connectionToken")
          )
          _ <- InstitutionGroupsDAO.addContact(group.id, contact.contactId)
        } yield contact

        query.transact(database).unsafeRunSync()
    }
  }

  def createGenericCredential(
      issuedBy: ParticipantId,
      contactId: Contact.Id,
      tag: String = "",
      credentialIssuanceContactId: Option[CredentialIssuance.ContactId] = None
  )(implicit
      database: Transactor[IO]
  ): GenericCredential = {

    def createRequest(credentialTypeId: CredentialTypeId) =
      CreateGenericCredential(
        credentialData = Json.obj(
          "title" -> s"Major In Applied Blockchain $tag".trim.asJson,
          "enrollmentDate" -> LocalDate.now().asJson,
          "graduationDate" -> LocalDate.now().plusYears(5).asJson
        ),
        credentialIssuanceContactId = credentialIssuanceContactId,
        credentialTypeId = credentialTypeId,
        contactId = Some(contactId),
        externalId = None
      )

    val credential = (for {
      credentialTypeWithRequiredFields <-
        CredentialTypeDao.create(issuedBy, sampleCreateCredentialType(s"Credential type $tag"))
      credential <-
        CredentialsDAO.create(issuedBy, contactId, createRequest(credentialTypeWithRequiredFields.credentialType.id))
    } yield credential).transact(database).unsafeRunSync()
    // Sleep 1 ms to ensure DB queries sorting by creation time are deterministic (this only happens during testing as
    // creating more than one credential by/to the same participant at the exact time is rather hard)
    Thread.sleep(1)
    credential
  }

  def createCredentialType(participantId: ParticipantId, name: String)(implicit
      database: Transactor[IO]
  ): CredentialTypeWithRequiredFields = {
    CredentialTypeDao.create(participantId, sampleCreateCredentialType(name)).transact(database).unsafeRunSync()
  }

  def sampleCreateCredentialType(name: String): CreateCredentialType = {
    CreateCredentialType(
      name = name,
      template = "",
      icon = None,
      fields = List(
        CreateCredentialTypeField(
          name = "title",
          description = "Title",
          `type` = CredentialTypeFieldType.String
        ),
        CreateCredentialTypeField(
          name = "enrollmentDate",
          description = "Date of the enrollment",
          `type` = CredentialTypeFieldType.Date
        ),
        CreateCredentialTypeField(
          name = "graduationDate",
          description = "Date of the graduation",
          `type` = CredentialTypeFieldType.Date
        )
      )
    )
  }

  def createReceivedCredential(contactId: Contact.Id)(implicit database: Transactor[IO]): Unit = {
    val request = ReceivedSignedCredentialData(
      contactId = contactId,
      credentialExternalId = CredentialExternalId(Random.alphanumeric.take(10).mkString("")),
      encodedSignedCredential = "signed-data-mock"
    )

    ReceivedCredentialsDAO.insertSignedCredential(request).transact(database).unsafeRunSync()
  }

  def newDID(): DID = {
    DID.createUnpublishedDID(EC.generateKeyPair().publicKey).canonical.value
  }

  def publishCredential(issuerId: ParticipantId, id: GenericCredential.Id)(implicit database: Transactor[IO]): Unit = {
    CredentialsDAO
      .storePublicationData(
        issuerId,
        PublishCredential(
          id,
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
      .transact(database)
      .unsafeRunSync()
    ()
  }

  def makeConnectionTokens(count: Int = 1): List[ConnectionToken] = {
    1.to(count).map(i => ConnectionToken(s"ConnectionToken$i")).toList
  }
}

package io.iohk.atala.prism.management.console

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{EC, MerkleInclusionProof, Sha256, Sha256Digest}
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.management.console.repositories.daos._
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.connector_models.ContactConnection
import io.iohk.atala.prism.protos.console_models.{ConnectorRequestMetadata, ContactConnectionStatus}

import java.time.{Instant, LocalDate}
import scala.util.Random
import scala.jdk.CollectionConverters._

object DataPreparation {
  def createParticipant(
      name: String
  )(implicit database: Transactor[IO]): ParticipantId = {
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

  val grpcAuthenticationHeaderDIDBased: GrpcAuthenticationHeader.DIDBased =
    GrpcAuthenticationHeader.PublishedDIDBased(
      did = newDID(),
      keyId = "didKeyId",
      signature = new ECSignature("didSignature".getBytes),
      requestNonce = RequestNonce("requestNonce".getBytes.toVector)
    )

  val connectorRequestMetadataProto: ConnectorRequestMetadata =
    ConnectorRequestMetadata(
      did = grpcAuthenticationHeaderDIDBased.did.toString,
      didKeyId = grpcAuthenticationHeaderDIDBased.keyId,
      didSignature = new String(grpcAuthenticationHeaderDIDBased.signature.getData),
      requestNonce = new String(grpcAuthenticationHeaderDIDBased.requestNonce.bytes.toArray)
    )

  def createInstitutionGroup(
      institutionId: ParticipantId,
      name: InstitutionGroup.Name
  )(implicit
      database: Transactor[IO]
  ): InstitutionGroup = {
    InstitutionGroupsDAO
      .create(institutionId, name)
      .transact(database)
      .unsafeRunSync()
  }

  def createContact(
      institutionId: ParticipantId,
      name: String = s"name-${Random.nextInt(100)}",
      groupName: Option[InstitutionGroup.Name] = None,
      createdAt: Option[Instant] = None,
      externalId: Contact.ExternalId = Contact.ExternalId.random(),
      connectionToken: String = "connectionToken"
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
      generateConnectionTokenRequestMetadata = grpcAuthenticationHeaderDIDBased
    )

    groupName match {
      case None =>
        ContactsDAO
          .createContact(
            institutionId,
            request,
            createdAt.getOrElse(Instant.now()),
            ConnectionToken(connectionToken)
          )
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
            ConnectionToken(connectionToken)
          )
          _ <- InstitutionGroupsDAO.addContact(group.id, contact.contactId)
        } yield contact

        query.transact(database).unsafeRunSync()
    }
  }

  def createContactWithConnectionStatus(
      name: String,
      connectionToken: String,
      connectionStatus: ContactConnectionStatus,
      institutionId: ParticipantId,
      groupName: InstitutionGroup.Name
  )(implicit
      database: Transactor[IO]
  ): (Contact, ContactConnection) = {
    (
      createContact(
        institutionId,
        name,
        Some(groupName),
        connectionToken = connectionToken
      ),
      ContactConnection(
        connectionToken = connectionToken,
        connectionStatus = connectionStatus
      )
    )
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
        CredentialTypeDao.create(
          issuedBy,
          sampleCreateCredentialType(s"Credential type $tag")
        )
      credential <-
        CredentialsDAO.create(
          issuedBy,
          contactId,
          createRequest(credentialTypeWithRequiredFields.credentialType.id)
        )
    } yield credential).transact(database).unsafeRunSync()
    // Sleep 1 ms to ensure DB queries sorting by creation time are deterministic (this only happens during testing as
    // creating more than one credential by/to the same participant at the exact time is rather hard)
    Thread.sleep(1)
    credential
  }

  def createCredentialType(participantId: ParticipantId, name: String)(implicit
      database: Transactor[IO]
  ): CredentialTypeWithRequiredFields = {
    CredentialTypeDao
      .create(participantId, sampleCreateCredentialType(name))
      .transact(database)
      .unsafeRunSync()
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

  def createReceivedCredential(
      contactId: Contact.Id
  )(implicit database: Transactor[IO]): Unit = {
    val merkleInclusionProof = MerkleInclusionProof.decode(
      """{"hash":"7d25e48be1c6475429bd33adbd5b7657340f264e62c2bf9b25ea478d9d3a2566","index":0,"siblings":[]}"""
    )
    val request = ReceivedSignedCredentialData(
      contactId = contactId,
      credentialExternalId = CredentialExternalId(Random.alphanumeric.take(10).mkString("")),
      encodedSignedCredential = "signed-data-mock",
      batchInclusionProof = Some(merkleInclusionProof)
    )

    ReceivedCredentialsDAO
      .insertSignedCredential(request)
      .transact(database)
      .unsafeRunSync()
  }

  def newDID(): DID = {
    DID
      .buildLongFormFromMasterPublicKey(
        EC.INSTANCE.generateKeyPair().getPublicKey
      )
      .asCanonical()
  }

  def publishCredential(
      issuerId: ParticipantId,
      batchId: CredentialBatchId,
      consoleId: GenericCredential.Id,
      encodedSignedCredential: String,
      mockMerkleProof: MerkleInclusionProof
  )(implicit database: Transactor[IO]): Unit = {
    CredentialsDAO
      .storePublicationData(
        issuerId,
        PublishCredential(
          consoleId,
          batchId,
          encodedSignedCredential,
          mockMerkleProof
        )
      )
      .transact(database)
      .unsafeRunSync()
    ()
  }

  def makeConnectionTokens(count: Int = 1): List[ConnectionToken] = {
    1.to(count).map(i => ConnectionToken(s"ConnectionToken$i")).toList
  }
  def getBatchData(
      batchId: CredentialBatchId
  )(implicit
      database: Transactor[IO]
  ): Option[(AtalaOperationId, Sha256Digest)] = {
    sql"""
         |SELECT issuance_operation_id, issuance_operation_hash
         |FROM published_batches
         |WHERE batch_id = ${batchId.getId}
         |""".stripMargin
      .query[(AtalaOperationId, Sha256Digest)]
      .option
      .transact(database)
      .unsafeRunSync()
  }

  def publishBatch(
      batchId: CredentialBatchId,
      previousOperationHash: Sha256Digest,
      atalaOperationId: AtalaOperationId
  )(implicit database: Transactor[IO]): Unit = {
    CredentialsDAO
      .storeBatchData(
        batchId,
        previousOperationHash,
        atalaOperationId
      )
      .transact(database)
      .unsafeRunSync()
    ()
  }

  def revokeCredential(
      issuerId: ParticipantId,
      credentialId: GenericCredential.Id,
      operationId: AtalaOperationId
  )(implicit
      database: Transactor[IO]
  ): Unit = {
    CredentialsDAO
      .revokeCredential(issuerId, credentialId, operationId)
      .transact(database)
      .unsafeRunSync()
  }

  def publishCredential(
      issuerId: ParticipantId,
      credential: GenericCredential
  )(implicit
      database: Transactor[IO]
  ): Unit = {
    val aHash = Sha256.compute("test".getBytes)
    val aBatchId = CredentialBatchId.random()
    val anEncodedSignedCredential = "mockEncodedSignedCredential"
    val aMerkleProof = new MerkleInclusionProof(aHash, 0, List().asJava)

    CredentialsDAO
      .storeBatchData(
        aBatchId,
        aHash,
        AtalaOperationId.fromVectorUnsafe(aHash.getValue.toVector)
      )
      .transact(database)
      .unsafeRunSync()
    CredentialsDAO
      .storePublicationData(
        issuerId,
        PublishCredential(
          credential.credentialId,
          aBatchId,
          anEncodedSignedCredential,
          aMerkleProof
        )
      )
      .transact(database)
      .unsafeRunSync()
    ()
  }

  def markAsRevoked(
    credentialId: GenericCredential.Id
  )(implicit database: Transactor[IO]): Unit = {
    val operationId = 1.to(64).map(_ => "a").mkString("")
    sql"""
          |UPDATE published_credentials
          |SET revoked_on_operation_id = decode($operationId, 'hex')
          |WHERE credential_id = ${credentialId.uuid.toString}::uuid
     """.stripMargin.update.run.void.transact(database).unsafeRunSync()
  }
}

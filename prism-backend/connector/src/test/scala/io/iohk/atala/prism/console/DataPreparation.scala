package io.iohk.atala.prism.console

import cats.effect.IO
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.connector.model.{ParticipantInfo, ParticipantType, TokenString}
import io.iohk.atala.prism.connector.repositories.{daos => connectorDaos}
import io.iohk.atala.prism.console.models._
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.console.repositories.{CredentialsRepository, daos => consoleDaos}
import io.iohk.atala.prism.crypto.{ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.migrations.Student.ConnectionStatus
import io.iohk.atala.prism.models.ParticipantId
import org.scalatest.OptionValues._

import java.time.LocalDate
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

object DataPreparation extends BaseDAO {

  import connectorDaos._
  import consoleDaos._

  def createIssuer(
      name: String = "Issuer",
      tag: String = "",
      publicKey: Option[ECPublicKey] = None,
      did: Option[DID] = None
  )(implicit
      database: Transactor[IO]
  ): Institution.Id = {
    val id = Institution.Id.random()
    val didValue = did.getOrElse(newDID())
    // dirty hack to create a participant while creating an issuer, TODO: Merge the tables
    val participant = ParticipantInfo(
      ParticipantId(id.uuid),
      ParticipantType.Issuer,
      publicKey,
      name + tag,
      Option(didValue),
      None,
      None
    )
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()

    id
  }

  def createVerifier(
      id: ParticipantId = ParticipantId.random(),
      name: String = "Verifier",
      tag: String = "",
      publicKey: Option[ECPublicKey] = None
  )(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    val participant =
      ParticipantInfo(id, ParticipantType.Verifier, publicKey, name + tag, Option(newDID()), None, None)
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()

    id
  }

  def createIssuerGroup(issuerId: Institution.Id, name: IssuerGroup.Name)(implicit
      database: Transactor[IO]
  ): IssuerGroup = {
    IssuerGroupsDAO.create(issuerId, name).transact(database).unsafeRunSync()
  }

  // Generic versions
  def createGenericCredential(issuedBy: Institution.Id, subjectId: Contact.Id, tag: String = "")(implicit
      database: Transactor[IO]
  ): GenericCredential = {
    val request = CreateGenericCredential(
      issuedBy = issuedBy,
      subjectId = subjectId,
      credentialData = Json.obj(
        "title" -> s"Major IN Applied Blockchain $tag".trim.asJson,
        "enrollmentDate" -> LocalDate.now().asJson,
        "graduationDate" -> LocalDate.now().plusYears(5).asJson
      ),
      groupName = s"Computer Science $tag".trim
    )

    // Add a millisecond delay to guarantee credentials are not created at the same time and ordering is guaranteed,
    // i.e., avoid test flakiness
    Thread.sleep(1)

    CredentialsDAO.create(request).transact(database).unsafeRunSync()
  }

  def createContact(issuerId: Institution.Id, subjectName: String, groupName: IssuerGroup.Name, tag: String = "")(
      implicit database: Transactor[IO]
  ): Contact = createContact(issuerId, subjectName, Some(groupName), tag)

  def createContact(issuerId: Institution.Id, subjectName: String, groupName: Option[IssuerGroup.Name], tag: String)(
      implicit database: Transactor[IO]
  ): Contact = {
    val request = CreateContact(
      createdBy = issuerId,
      data = Json.obj(
        "universityAssignedId" -> s"uid - $tag".asJson,
        "full_name" -> subjectName.asJson,
        "email" -> "donthaveone@here.com".asJson,
        "admissionDate" -> LocalDate.now().asJson
      ),
      externalId = Contact.ExternalId.random()
    )

    groupName match {
      case None =>
        ContactsDAO.createContact(request).transact(database).unsafeRunSync()
      case Some(name) =>
        val group = IssuerGroupsDAO
          .find(issuerId, name)
          .transact(database)
          .unsafeRunSync()
          .getOrElse(throw new RuntimeException(s"Group $name does not exist"))

        val query = for {
          contact <- ContactsDAO.createContact(request)
          _ <- IssuerGroupsDAO.addContact(group.id, contact.contactId)
        } yield contact

        query.transact(database).unsafeRunSync()
    }
  }

  def generateConnectionToken(issuerId: Institution.Id, contactId: Contact.Id, maybeToken: Option[TokenString] = None)(
      implicit database: Transactor[IO]
  ): TokenString = {
    val token = maybeToken.getOrElse(TokenString.random())

    val tx = for {
      _ <- ConnectionTokensDAO.insert(ParticipantId(issuerId.uuid), List(token))
      _ <- ContactsDAO.setConnectionToken(issuerId, contactId, token)
    } yield token

    tx.transact(database).unsafeRunSync()
  }

  // the difference from the ContactsDAO method is that this one doesn't set a connection id
  def acceptConnection(issuerId: Institution.Id, token: TokenString)(implicit database: Transactor[IO]): Unit = {
    sql"""
         |UPDATE contacts
         |SET connection_status = ${ConnectionStatus.ConnectionAccepted: ConnectionStatus}::CONTACT_CONNECTION_STATUS_TYPE
         |WHERE connection_token = $token AND
         |      created_by = $issuerId
         |""".stripMargin.update.run.void
      .transact(database)
      .unsafeRunSync()
  }

  def newDID(): DID = {
    DID.createUnpublishedDID(EC.generateKeyPair().publicKey).canonical.value
  }

  def readMerkleProof(batchId: CredentialBatchId)(implicit database: Transactor[IO]): Option[MerkleInclusionProof] = {
    sql"""
         | SELECT inclusion_proof_hash, inclusion_proof_index, inclusion_proof_siblings
         | FROM published_batches
         | WHERE batch_id = $batchId
         |""".stripMargin
      .query[MerkleInclusionProof]
      .option
      .transact(database)
      .unsafeRunSync()
  }

  def publishBatch(
      batchId: CredentialBatchId,
      previousOperationHash: SHA256Digest,
      atalaOperationId: AtalaOperationId
  )(implicit credentialsRepository: CredentialsRepository): Unit = {
    credentialsRepository
      .storeBatchData(
        StoreBatchData(
          batchId,
          previousOperationHash,
          atalaOperationId
        )
      )
      .value
      .futureValue
    ()
  }

  def publishCredential(
      issuerId: Institution.Id,
      batchId: CredentialBatchId,
      consoleId: GenericCredential.Id,
      encodedSignedCredential: String,
      mockMerkleProof: MerkleInclusionProof
  )(implicit credentialsRepository: CredentialsRepository): Unit = {
    credentialsRepository
      .storeCredentialPublicationData(
        issuerId,
        CredentialPublicationData(
          consoleId,
          batchId,
          encodedSignedCredential,
          mockMerkleProof
        )
      )
      .value
      .futureValue
    ()
  }

  def getBatchData(
      batchId: CredentialBatchId
  )(implicit database: Transactor[IO]): Option[SHA256Digest] = {
    sql"""
         |SELECT issuance_operation_hash
         |FROM published_batches
         |WHERE batch_id = $batchId
         |""".stripMargin
      .query[SHA256Digest]
      .option
      .transact(database)
      .unsafeRunSync()
  }
}

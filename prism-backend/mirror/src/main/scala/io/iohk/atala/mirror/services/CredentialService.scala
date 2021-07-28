package io.iohk.atala.mirror.services

import scala.concurrent.duration.DurationInt
import scala.util.Try
import cats.data.{EitherT, ValidatedNel}
import cats.data.Validated.{Invalid, Valid}
import doobie.util.transactor.Transactor
import fs2.Stream
import monix.eval.Task
import org.slf4j.LoggerFactory
import io.iohk.atala.mirror.db.{ConnectionDao, UserCredentialDao}
import io.iohk.atala.mirror.models.UserCredential.{CredentialStatus, MessageReceivedDate, RawCredential}
import io.iohk.atala.prism.models.{
  ConnectionId,
  ConnectionState,
  ConnectionToken,
  ConnectorMessageId,
  CredentialProofRequestType
}
import io.iohk.atala.mirror.models.{Connection, UserCredential}
import io.iohk.atala.prism.credentials.{
  BatchData,
  Credential,
  CredentialBatchId,
  PrismCredentialVerification,
  TimestampInfo,
  VerificationError
}
import io.iohk.atala.prism.crypto.{EC, MerkleTree, SHA256Digest}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.node_api.GetBatchStateResponse
import cats.implicits._
import doobie.implicits._
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.credential_models.{AtalaMessage, PlainTextCredential}
import io.iohk.atala.prism.services.{ConnectorClientService, MessageProcessor, NodeClientService}
import io.iohk.atala.prism.services.MessageProcessor.MessageProcessorResult
import io.iohk.atala.prism.utils.ConnectionUtils
import io.iohk.atala.prism.utils.syntax._

class CredentialService(
    tx: Transactor[Task],
    connectorService: ConnectorClientService,
    nodeService: NodeClientService
) {

  implicit val ec = EC

  private val logger = LoggerFactory.getLogger(classOf[CredentialService])

  private val GET_CONNECTIONS_PAGINATED_LIMIT = 100
  private val GET_CONNECTIONS_PAGINATED_AWAKE_DELAY = 11.seconds

  /**
    * @param immediatelyRequestedCredential Request credential proof in this place is temporary and probably will be removed in the future.
    */
  def connectionUpdatesStream(immediatelyRequestedCredential: CredentialProofRequestType): Stream[Task, Unit] = {
    Stream
      .eval(
        ConnectionDao.findLastSeenConnectionId.logSQLErrors(s"finding last connection id", logger).transact(tx)
      )
      .flatMap(lastSeenConnectionId =>
        connectorService
          .getConnectionsPaginatedStream(
            lastSeenConnectionId,
            GET_CONNECTIONS_PAGINATED_LIMIT,
            GET_CONNECTIONS_PAGINATED_AWAKE_DELAY
          )
          .evalMap(connectionInfo => {
            val connection = Connection(
              token = ConnectionToken(connectionInfo.token),
              id = ConnectionId.from(connectionInfo.connectionId).toOption,
              state = ConnectionState.Connected,
              holderDID = DID.fromString(connectionInfo.participantDid),
              payIdName = None
            )

            ConnectionDao
              .update(connection)
              .logSQLErrors("updating connection", logger)
              .transact(tx)
              .as(connection)
          })
          .evalMap(connection => {
            for {
              _ <- connection.id match {
                case None => Task.unit
                case Some(connectionId) =>
                  connectorService
                    .requestCredential(
                      connectionId = connectionId,
                      connectionToken = connection.token,
                      credentialProofRequestTypes = Seq(immediatelyRequestedCredential)
                    )
              }

              _ = logger.info(s"Request credential: $immediatelyRequestedCredential")
            } yield ()
          })
          .drain // discard return type, as we don't need it
      )
  }

  val credentialMessageProcessor: MessageProcessor = { receivedMessage =>
    parseCredential(receivedMessage)
      .map(saveMessage(receivedMessage, _))
  }

  private def saveMessage(
      receivedMessage: ReceivedMessage,
      plainTextCredential: PlainTextCredential
  ): MessageProcessorResult = {
    (for {
      connection <- EitherT(
        ConnectionUtils
          .fromReceivedMessage(receivedMessage, ConnectionDao.findByConnectionId)
          .logSQLErrors("getting connection from received message", logger)
          .transact(tx)
      )
      userCredentials <-
        EitherT
          .liftF(createUserCredential(receivedMessage, connection.token, plainTextCredential))
      _ <-
        EitherT
          .liftF[Task, PrismError, Int](
            UserCredentialDao.insert(userCredentials).logSQLErrors("inserting user credentials", logger).transact(tx)
          )
    } yield None).value
  }

  private[services] def parseCredential(message: ReceivedMessage): Option[PlainTextCredential] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray).getPlainCredential).toOption
      .filter(credential => credential.encodedCredential.nonEmpty)
      .orElse {
        Try(PlainTextCredential.parseFrom(message.message.toByteArray)).toOption
          .filter(credential => credential.encodedCredential.nonEmpty)
      }
  }

  private[services] def getIssuersDid(rawCredential: RawCredential): Option[DID] = {
    Credential
      .fromString(rawCredential.rawCredential)
      .flatMap(_.content.issuerDid)
      .toOption
  }

  private def createUserCredential(
      receivedMessage: ReceivedMessage,
      token: ConnectionToken,
      plainTextCredential: PlainTextCredential
  ): Task[UserCredential] = {
    verifyCredential(plainTextCredential).map { result =>
      val credentialStatus = result match {
        case Right(verificationResult) => parseCredentialStatus(verificationResult)
        case Left(parsingError) =>
          logger.warn(parsingError)
          CredentialStatus.Received
      }

      UserCredential(
        token,
        RawCredential(plainTextCredential.encodedCredential),
        getIssuersDid(RawCredential(plainTextCredential.encodedCredential)),
        ConnectorMessageId(receivedMessage.id),
        MessageReceivedDate(
          receivedMessage.received.getOrElse(throw new RuntimeException("Missing timestamp")).toInstant
        ),
        credentialStatus
      )
    }
  }

  private def parseCredentialStatus(result: ValidatedNel[VerificationError, Unit]): CredentialStatus =
    result match {
      case Valid(_) => CredentialStatus.Valid
      case Invalid(errors) =>
        logger.warn(s"Parse credential error: $errors")
        errors.head match {
          case _: VerificationError.CredentialWasRevoked => CredentialStatus.Revoked
          case _: VerificationError.KeyWasNotValid => CredentialStatus.Invalid
          case _: VerificationError.KeyWasRevoked => CredentialStatus.Invalid
          case VerificationError.InvalidSignature => CredentialStatus.Invalid
          // Added for exhaustive analysis, related to credentials batches
          case _: VerificationError.BatchWasRevoked => CredentialStatus.Revoked
          case VerificationError.InvalidMerkleProof => CredentialStatus.Invalid
        }
    }

  private[services] def verifyCredential(
      plainTextCredential: PlainTextCredential
  ): Task[Either[String, ValidatedNel[VerificationError, Unit]]] = {
    (for {
      credential <- Credential.fromString(plainTextCredential.encodedCredential).left.map(_.message).toEitherT[Task]
      issuerDid <- credential.content.issuerDid.left.map(_.getMessage).toEitherT[Task]
      issuanceKeyId <- credential.content.issuanceKeyId.left.map(_.getMessage).toEitherT[Task]
      keyData <- NodeClientService.getKeyData(issuerDid, issuanceKeyId, nodeService)
      merkleInclusionProof <- getMerkleInclusionProof(credential, plainTextCredential)
      batchId = CredentialBatchId.fromBatchData(issuerDid.suffix, merkleInclusionProof.derivedRoot)
      batchData <- getBatchData(batchId)
      credentialRevocationTime <- getCredentialRevocationTime(batchId, credential.hash)
    } yield PrismCredentialVerification
      .verify(
        keyData,
        batchData,
        credentialRevocationTime,
        merkleInclusionProof.derivedRoot,
        merkleInclusionProof,
        credential
      )).value
  }

  def getMerkleInclusionProof(
      credential: Credential,
      plainTextCredential: PlainTextCredential
  ): EitherT[Task, String, MerkleTree.MerkleInclusionProof] = {
    Either
      .fromOption(
        MerkleInclusionProof.decode(plainTextCredential.encodedMerkleProof),
        s"Credential $credential is missing inclusion proof"
      )
      .toEitherT[Task]
  }

  private[services] def getCredentialRevocationTime(
      credentialBatchId: CredentialBatchId,
      credentialHash: SHA256Digest
  ): EitherT[Task, String, Option[TimestampInfo]] = {
    EitherT[Task, String, Option[TimestampInfo]](
      nodeService.getCredentialRevocationTime(credentialBatchId, credentialHash).map { response =>
        Right {
          response.revocationLedgerData
            .flatMap(_.timestampInfo)
            .map(NodeClientService.fromTimestampInfoProto)
        }
      }
    )
  }

  private[services] def getBatchData(credentialBatchId: CredentialBatchId): EitherT[Task, String, BatchData] = {
    for {
      response <- EitherT[Task, String, GetBatchStateResponse](
        nodeService
          .getBatchState(credentialBatchId)
          .map(_.toRight(s"Credential batch with id: $credentialBatchId not found"))
      )

      batchIssuanceDate <-
        Either
          .fromOption(
            response.publicationLedgerData.flatMap(_.timestampInfo).map(NodeClientService.fromTimestampInfoProto),
            s"Credential batch: $credentialBatchId doesn't contain publication ledger data"
          )
          .toEitherT[Task]

      batchRevocationDate =
        response.revocationLedgerData.flatMap(_.timestampInfo).map(NodeClientService.fromTimestampInfoProto)

    } yield BatchData(batchIssuanceDate, batchRevocationDate)
  }
}

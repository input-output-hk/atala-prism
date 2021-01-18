package io.iohk.atala.mirror.services

import java.time.Instant

import scala.concurrent.duration.DurationInt
import scala.util.Try
import cats.data.{EitherT, ValidatedNel}
import cats.data.Validated.{Invalid, Valid}
import doobie.util.transactor.Transactor
import fs2.Stream
import monix.eval.Task
import org.slf4j.LoggerFactory
import io.iohk.atala.prism.utils.UUIDUtils.parseUUID
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
  Credential,
  CredentialData,
  PrismCredentialVerification,
  SlayerCredentialId,
  VerificationError
}
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.node_api.GetCredentialStateResponse
import cats.implicits._
import doobie.implicits._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.services.{ConnectorClientService, NodeClientService, MessageProcessor}
import io.iohk.atala.prism.services.MessageProcessor.{MessageProcessorResult, MessageProcessorException}

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
        ConnectionDao.findLastSeenConnectionId.transact(tx)
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
              id = parseUUID(connectionInfo.connectionId).map(ConnectionId),
              state = ConnectionState.Connected,
              holderDID = DID.fromString(connectionInfo.participantDID),
              payIdName = None
            )

            ConnectionDao
              .update(connection)
              .transact(tx)
              .map(_ => connection)
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

  private def saveMessage(receivedMessage: ReceivedMessage, rawCredential: RawCredential): MessageProcessorResult = {
    (for {
      connection <- EitherT(Connection.fromReceivedMessage(receivedMessage).transact(tx))
      userCredentials <-
        EitherT
          .liftF(createUserCredential(receivedMessage, connection.token, rawCredential))
          .leftMap(MessageProcessorException.apply)
      _ <-
        EitherT
          .liftF(UserCredentialDao.insert(userCredentials).transact(tx))
          .leftMap(MessageProcessorException.apply)
    } yield ()).value
  }

  private[services] def parseCredential(message: ReceivedMessage): Option[RawCredential] = {
    Try(credential_models.Credential.parseFrom(message.message.toByteArray)).toOption
      .map(_.credentialDocument)
      .filterNot(_.isEmpty)
      .map(RawCredential)
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
      rawCredential: RawCredential
  ): Task[UserCredential] = {
    verifyCredential(rawCredential.rawCredential).map { result =>
      val credentialStatus = result match {
        case Right(verificationResult) => parseCredentialStatus(verificationResult)
        case Left(parsingError) =>
          logger.warn(parsingError)
          CredentialStatus.Received
      }

      UserCredential(
        token,
        rawCredential,
        getIssuersDid(rawCredential),
        ConnectorMessageId(receivedMessage.id),
        MessageReceivedDate(Instant.ofEpochMilli(receivedMessage.received)),
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
      signedCredentialStringRepresentation: String
  ): Task[Either[String, ValidatedNel[VerificationError, Unit]]] = {
    (for {
      credential <- Credential.fromString(signedCredentialStringRepresentation).left.map(_.message).toEitherT[Task]
      issuerDid <- credential.content.issuerDid.left.map(_.getMessage).toEitherT[Task]
      issuanceKeyId <- credential.content.issuanceKeyId.left.map(_.getMessage).toEitherT[Task]
      keyData <- NodeClientService.getKeyData(issuerDid, issuanceKeyId, nodeService)
      credentialData <- getCredentialData(SlayerCredentialId.compute(credential.hash, issuerDid))
    } yield PrismCredentialVerification.verify(keyData, credentialData, credential)).value
  }

  private[services] def getCredentialData(id: SlayerCredentialId): EitherT[Task, String, CredentialData] = {
    for {
      response <- EitherT[Task, String, GetCredentialStateResponse](
        nodeService.getCredentialState(id.string).map(Right(_))
      )
      publishedOn <-
        response.publicationDate
          .map(NodeClientService.fromTimestampInfoProto)
          .toRight(s"Missing publication date ${id.string}")
          .toEitherT[Task]

      revokedOn = response.revocationDate map NodeClientService.fromTimestampInfoProto
    } yield CredentialData(issuedOn = publishedOn, revokedOn = revokedOn)
  }
}

package io.iohk.atala.mirror.services

import java.time.Instant

import scala.concurrent.duration.DurationInt
import scala.util.Try
import cats.data.{EitherT, OptionT, ValidatedNel}
import cats.data.Validated.{Invalid, Valid}
import doobie.util.transactor.Transactor
import fs2.Stream
import monix.eval.Task
import org.slf4j.LoggerFactory
import io.iohk.atala.mirror.NodeUtils.fromTimestampInfoProto
import io.iohk.atala.mirror.Utils.parseUUID
import io.iohk.atala.mirror.db.{ConnectionDao, UserCredentialDao}
import io.iohk.atala.mirror.models.UserCredential.{CredentialStatus, MessageReceivedDate, RawCredential}
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.mirror.models.{Connection, ConnectorMessageId, CredentialProofRequestType, UserCredential}
import io.iohk.atala.prism.credentials.{CredentialData, SlayerCredentialId, VerifiableCredential, VerificationError}
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.node_api.GetCredentialStateResponse
import cats.implicits._
import doobie.implicits._
import io.iohk.atala.mirror.NodeUtils
import io.iohk.atala.mirror.utils.ConnectionUtils
import io.iohk.atala.prism.credentials.json.implicits._
import io.iohk.atala.prism.identity.DID

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
              holderDID = DID.fromString(connectionInfo.participantDID)
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

  val credentialMessageProcessor: MessageProcessor = new MessageProcessor {
    def attemptProcessMessage(receivedMessage: ReceivedMessage): Option[Task[Unit]] = {
      parseCredential(receivedMessage).map(rawCredential => saveMessage(receivedMessage, rawCredential))
    }
  }

  private def saveMessage(receivedMessage: ReceivedMessage, rawCredential: RawCredential): Task[Unit] = {
    (for {
      connection <- OptionT(ConnectionUtils.findConnection(receivedMessage, logger).transact(tx))
      userCredentials <- OptionT.liftF(createUserCredential(receivedMessage, connection.token, rawCredential))
      _ <- OptionT.liftF(UserCredentialDao.insert(userCredentials).transact(tx))
    } yield ()).value.map(_ => ())
  }

  private[services] def parseCredential(message: ReceivedMessage): Option[RawCredential] = {
    Try(credential_models.Credential.parseFrom(message.message.toByteArray)).toOption
      .map(_.credentialDocument)
      .filter(!_.isEmpty)
      .map(RawCredential)
  }

  private[services] def getIssuersDid(rawCredential: RawCredential): Option[DID] = {
    JsonBasedCredential
      .fromString(rawCredential.rawCredential)
      .toOption
      .flatMap(_.content.issuerDid)
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
          case _: VerificationError.Revoked => CredentialStatus.Revoked
          case _: VerificationError.KeyWasNotValid => CredentialStatus.Invalid
          case _: VerificationError.KeyWasRevoked => CredentialStatus.Invalid
          case VerificationError.InvalidSignature => CredentialStatus.Invalid
        }
    }

  private[services] def verifyCredential(
      signedCredentialStringRepresentation: String
  ): Task[Either[String, ValidatedNel[VerificationError, Unit]]] = {
    (for {
      credential <-
        JsonBasedCredential.fromString(signedCredentialStringRepresentation).left.map(_.message).toEitherT[Task]
      issuerDid <- EitherT.fromOption[Task](credential.content.issuerDid, "Empty issuerDID")
      issuanceKeyId <- EitherT.fromOption[Task](credential.content.issuanceKeyId, "Empty issuanceKeyId")
      keyData <- NodeUtils.getKeyData(issuerDid, issuanceKeyId, nodeService)
      credentialData <- getCredentialData(SlayerCredentialId.compute(credential.hash, issuerDid))
    } yield VerifiableCredential.verify(keyData, credentialData, credential)).value
  }

  private[services] def getCredentialData(id: SlayerCredentialId): EitherT[Task, String, CredentialData] = {
    for {
      response <- EitherT[Task, String, GetCredentialStateResponse](
        nodeService.getCredentialState(id.string).map(Right(_))
      )
      publishedOn <-
        response.publicationDate
          .map(fromTimestampInfoProto)
          .toRight(s"Missing publication date ${id.string}")
          .toEitherT[Task]

      revokedOn = response.revocationDate map fromTimestampInfoProto
    } yield CredentialData(issuedOn = publishedOn, revokedOn = revokedOn)
  }
}

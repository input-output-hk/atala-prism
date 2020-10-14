package io.iohk.atala.mirror.services

import scala.concurrent.duration.DurationInt
import scala.util.Try

import cats.data.{EitherT, NonEmptyList, ValidatedNel}
import monix.eval.Task
import doobie.util.transactor.Transactor
import io.iohk.prism.protos.credential_models
import io.iohk.atala.mirror.db.{ConnectionDao, UserCredentialDao}
import io.iohk.atala.mirror.models.{Connection, CredentialProofRequestType}
import io.iohk.atala.mirror.models.Connection.ConnectionState
import io.iohk.atala.mirror.models.UserCredential
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken}
import io.iohk.atala.mirror.models.UserCredential.{
  CredentialStatus,
  IssuersDID,
  MessageId,
  MessageReceivedDate,
  RawCredential
}
import io.iohk.prism.protos.connector_models.ReceivedMessage
import java.time.Instant

import io.iohk.atala.credentials.{
  CredentialData,
  CredentialVerification,
  CredentialsCryptoSDKImpl,
  JsonBasedUnsignedCredential,
  KeyData,
  SignedCredential,
  UnsignedCredential,
  UnsignedCredentialBuilder
}
import io.iohk.atala.crypto.EC
import JsonBasedUnsignedCredential.jsonBasedUnsignedCredential
import cats.data.Validated.{Invalid, Valid}
import fs2.Stream
import io.iohk.atala.mirror.Utils.parseUUID
import org.slf4j.LoggerFactory
import io.iohk.atala.credentials.VerificationError
import io.iohk.atala.mirror.NodeUtils.{computeNodeCredentialId, fromProtoKey, fromTimestampInfoProto}
import io.iohk.prism.protos.node_api.GetCredentialStateResponse

import cats.implicits._
import doobie.implicits._

class CredentialService(
    tx: Transactor[Task],
    connectorService: ConnectorClientService,
    nodeService: NodeClientService
) {

  private val logger = LoggerFactory.getLogger(classOf[CredentialService])

  private val GET_MESSAGES_PAGINATED_LIMIT = 100
  private val GET_MESSAGES_PAGINATED_AWAKE_DELAY = 10.seconds

  private val GET_CONNECTIONS_PAGINATED_LIMIT = 100
  private val GET_CONNECTIONS_PAGINATED_AWAKE_DELAY = 11.seconds

  val credentialUpdatesStream: Stream[Task, Seq[ReceivedMessage]] = {
    Stream
      .eval(UserCredentialDao.findLastSeenMessageId.transact(tx))
      .flatMap { lastSeenMessageId =>
        connectorService.getMessagesPaginatedStream(
          lastSeenMessageId,
          GET_MESSAGES_PAGINATED_LIMIT,
          GET_MESSAGES_PAGINATED_AWAKE_DELAY
        )
      }
      .evalTap(saveMessages)
  }

  val connectionUpdatesStream: Stream[Task, Unit] = {
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
              state = ConnectionState.Connected
            )

            ConnectionDao
              .update(connection)
              .transact(tx)
              .map(_ => connection)
          })
          .evalMap(connection => {
            // TODO: Request credential in this place is temporary and
            //       probably will be removed in the future.
            val credential = CredentialProofRequestType.RedlandIdCredential

            for {
              _ <- connection.id match {
                case None => Task.unit
                case Some(connectionId) =>
                  connectorService
                    .requestCredential(
                      connectionId = connectionId,
                      connectionToken = connection.token,
                      credentialProofRequestTypes = Seq(credential)
                    )
              }

              _ = logger.info(s"Request credential: $credential")
            } yield ()
          })
          .drain // discard return type, as we don't need it
      )
  }

  private def saveMessages(messages: Seq[ReceivedMessage]): Task[Unit] = {
    val connectionIds = parseConnectionIds(messages)

    for {
      connections <-
        NonEmptyList
          .fromList(connectionIds.toList)
          .map(ids => ConnectionDao.findBy(ids).transact(tx))
          .getOrElse(Task.pure(Nil))

      connectionIdToTokenMap =
        connections
          .flatMap(connection => connection.id.map(_.uuid.toString -> connection.token))
          .toMap

      userCredentials <- Task.sequence(messages.flatMap { receivedMessage =>
        for {
          rawCredential <- parseCredential(receivedMessage)
          token <- connectionIdToTokenMap.get(receivedMessage.connectionId).orElse {
            logger.warn(
              s"Message with id: ${receivedMessage.id} and connectionId ${receivedMessage.connectionId}" +
                s"does not have corresponding connection or connection does not have connectionId, skipping it."
            )
            None
          }
        } yield createUserCredential(receivedMessage, token, rawCredential)
      }.toList)

      _ <- UserCredentialDao.insertMany.updateMany(userCredentials).transact(tx)
    } yield ()
  }

  private[services] def parseConnectionIds(messages: Seq[ReceivedMessage]): Seq[ConnectionId] = {
    messages.flatMap { receivedMessage =>
      parseConnectionId(receivedMessage.connectionId).orElse {
        logger.warn(
          s"Message with id: ${receivedMessage.id} has incorrect connectionId. ${receivedMessage.connectionId} " +
            s"is not valid UUID"
        )
        None
      }
    }
  }

  private[services] def parseCredential(message: ReceivedMessage): Option[RawCredential] = {
    Try(credential_models.Credential.parseFrom(message.message.toByteArray)).toOption
      .map(_.credentialDocument)
      .map(RawCredential)
  }

  private[services] def parseConnectionId(connectionId: String): Option[ConnectionId] =
    parseUUID(connectionId).map(ConnectionId)

  private[services] def getIssuersDid(rawCredential: RawCredential): Option[IssuersDID] = {
    val unsignedCredential: UnsignedCredential = SignedCredential.from(rawCredential.rawCredential).toOption match {
      case Some(signedCredential) =>
        signedCredential.decompose[JsonBasedUnsignedCredential].credential
      case None =>
        UnsignedCredentialBuilder[JsonBasedUnsignedCredential]
          .fromBytes(rawCredential.rawCredential.getBytes)
    }

    unsignedCredential.issuerDID.map(IssuersDID)
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
        MessageId(receivedMessage.id),
        MessageReceivedDate(Instant.ofEpochMilli(receivedMessage.received)),
        credentialStatus
      )
    }
  }

  private def parseCredentialStatus(result: ValidatedNel[VerificationError, Unit]): CredentialStatus =
    result match {
      case Valid(_) => CredentialStatus.Valid
      case Invalid(errors) =>
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
      signedCredential <-
        SignedCredential.from(signedCredentialStringRepresentation).toEither.left.map(_.getMessage).toEitherT[Task]
      unsignedCredential = signedCredential.decompose[JsonBasedUnsignedCredential].credential
      issuerDID <- unsignedCredential.issuerDID.toRight("issuerDID is missing in received credential").toEitherT[Task]
      issuanceKeyId <-
        unsignedCredential.issuanceKeyId.toRight("issuanceKeyId is missing in received credential").toEitherT[Task]
      keyData <- getKeyData(issuerDID = issuerDID, issuanceKeyId = issuanceKeyId)
      credentialData <- getCredentialData(
        computeNodeCredentialId(
          credentialHash = CredentialsCryptoSDKImpl.hash(signedCredential),
          did = issuerDID
        )
      )
    } yield CredentialVerification.verifyCredential(keyData, credentialData, signedCredential)(EC)).value
  }

  private[services] def getKeyData(issuerDID: String, issuanceKeyId: String): EitherT[Task, String, KeyData] = {
    for {
      didData <- EitherT(nodeService.getDidDocument(issuerDID).map(_.toRight(s"DID Data not found for DID $issuerDID")))

      issuingKeyProto <-
        didData.publicKeys
          .find(_.id == issuanceKeyId)
          .toRight(s"KeyId not found: $issuanceKeyId")
          .toEitherT[Task]

      issuingKey <- fromProtoKey(issuingKeyProto)
        .toRight(s"Failed to parse proto key: $issuingKeyProto")
        .toEitherT[Task]

      addedOn <-
        issuingKeyProto.addedOn
          .map(fromTimestampInfoProto)
          .toRight(s"Missing addedOn time:\n-Issuer DID: $issuerDID\n- keyId: $issuanceKeyId ")
          .toEitherT[Task]

      revokedOn = issuingKeyProto.revokedOn.map(fromTimestampInfoProto)
    } yield KeyData(publicKey = issuingKey, addedOn = addedOn, revokedOn = revokedOn)
  }

  private[services] def getCredentialData(nodeCredentialId: String): EitherT[Task, String, CredentialData] = {
    for {
      response <- EitherT[Task, String, GetCredentialStateResponse](
        nodeService.getCredentialState(nodeCredentialId).map(Right(_))
      )
      publishedOn <-
        response.publicationDate
          .map(fromTimestampInfoProto)
          .toRight(s"Missing publication date $nodeCredentialId")
          .toEitherT[Task]

      revokedOn = response.revocationDate map fromTimestampInfoProto
    } yield CredentialData(issuedOn = publishedOn, revokedOn = revokedOn)
  }

}

package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.ManagementConsoleErrorSupport
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs.genericCredentialToProto
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.{ContactsRepository, CredentialsRepository}
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CredentialsServiceImpl(
    credentialsRepository: CredentialsRepository,
    contactsRepository: ContactsRepository,
    authenticator: ManagementConsoleAuthenticator,
    nodeService: NodeServiceGrpc.NodeService
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialsServiceGrpc.CredentialsService
    with ManagementConsoleErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGenericCredential(
      request: CreateGenericCredentialRequest
  ): Future[CreateGenericCredentialResponse] =
    authenticatedHandler("createGenericCredential", request) { issuerId =>
      // get the subjectId from the externalId
      // TODO: Avoid doing this when we stop accepting the subjectId
      val subjectIdF = Option(request.externalId)
        .filter(_.nonEmpty)
        .map(Contact.ExternalId.apply)
        .fold {
          Future
            .successful(Contact.Id.from(request.contactId).toEither)
            .toFutureEither(
              new RuntimeException("The contactId is required, if it was provided, it's an invalid value")
            )
        } { externalId =>
          contactsRepository
            .find(ParticipantId(issuerId), externalId)
            .map(_.getOrElse(throw new RuntimeException("The given externalId doesn't exist")))
            .map(_.contactId)
        }

      lazy val json =
        io.circe.parser.parse(request.credentialData).getOrElse(throw new RuntimeException("Invalid json"))

      val result = for {
        contactId <- subjectIdF
        model =
          request
            .into[CreateGenericCredential]
            .withFieldConst(_.issuedBy, ParticipantId(issuerId))
            .withFieldConst(_.subjectId, contactId)
            .withFieldConst(_.credentialData, json)
            .withFieldConst(_.credentialIssuanceContactId, None)
            .withFieldConst(
              _.credentialTypeId,
              if (request.credentialTypeId.isEmpty) None
              else CredentialTypeId.from(request.credentialTypeId).toOption
            )
            .enableUnsafeOption
            .transform

        created <-
          credentialsRepository
            .create(model)
            .map(genericCredentialToProto)
      } yield console_api.CreateGenericCredentialResponse().withGenericCredential(created)

      result.failOnLeft(identity)
    }

  override def getGenericCredentials(
      request: GetGenericCredentialsRequest
  ): Future[GetGenericCredentialsResponse] =
    authenticatedHandler("getGenericCredentials", request) { issuerId =>
      val lastSeenCredential = GenericCredential.Id.from(request.lastSeenCredentialId).toOption
      credentialsRepository
        .getBy(ParticipantId(issuerId), request.limit, lastSeenCredential)
        .map { list =>
          console_api.GetGenericCredentialsResponse(list.map(genericCredentialToProto))
        }
    }

  private def authenticatedHandler[Request <: scalapb.GeneratedMessage, Response <: scalapb.GeneratedMessage](
      methodName: String,
      request: Request
  )(
      block: UUID => FutureEither[Nothing, Response]
  ): Future[Response] = {
    authenticator.authenticated(methodName, request) { participantId =>
      block(participantId.uuid).value
        .map {
          case Right(x) => x
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
  }

  override def getContactCredentials(request: GetContactCredentialsRequest): Future[GetContactCredentialsResponse] = {
    def f(institutionId: ParticipantId): Future[GetContactCredentialsResponse] = {
      for {
        contactId <- Future.fromTry(Contact.Id.from(request.contactId))
        response <-
          credentialsRepository
            .getBy(institutionId, contactId)
            .map { list =>
              console_api.GetContactCredentialsResponse(list.map(genericCredentialToProto))
            }
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
      } yield response
    }

    authenticator.authenticated("getSubjectCredentials", request) { participantId =>
      f(participantId)
    }
  }

  override def shareCredential(request: ShareCredentialRequest): Future[ShareCredentialResponse] = {
    def f(institutionId: ParticipantId): Future[ShareCredentialResponse] = {
      for {
        credentialId <- Future.fromTry(GenericCredential.Id.from(request.cmanagerCredentialId))
        response <-
          credentialsRepository
            .markAsShared(institutionId, credentialId)
            .map { _ =>
              console_api.ShareCredentialResponse()
            }
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
      } yield response
    }

    authenticator.authenticated("shareCredential", request) { participantId =>
      f(participantId)
    }
  }

  /** Retrieves node information associated to a credential
    */
  override def getBlockchainData(request: GetBlockchainDataRequest): Future[GetBlockchainDataResponse] = ???

  /** Publishes a credential batch to the blockchain.
    * This method also stores the published batch on the database.
    * However, it does not store the individual credentials on the console
    */
  override def publishBatch(request: PublishBatchRequest): Future[PublishBatchResponse] = {
    println(nodeService.toString) // added only to remove "no use" error
    ???
  }

  /** This request stores in the console database the information associated to a credential.
    * The endpoint assumes that the credential has been publish in a batch though the PublishBatch endpoint
    */
  override def storePublishedCredential(
      request: StorePublishedCredentialRequest
  ): Future[StorePublishedCredentialResponse] = ???
}

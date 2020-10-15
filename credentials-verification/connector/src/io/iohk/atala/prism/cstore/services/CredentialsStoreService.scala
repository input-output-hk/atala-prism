package io.iohk.atala.prism.cstore.services

import java.util.UUID

import io.iohk.atala.prism.connector.Authenticator
import io.iohk.atala.prism.connector.errors.{ErrorSupport, LoggingContext}
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution}
import io.iohk.atala.prism.console.repositories.{ContactsRepository, StoredCredentialsRepository}
import io.iohk.atala.prism.console.repositories.daos.StoredCredentialsDAO.StoredSignedCredentialData
import io.iohk.atala.prism.cstore.grpc.ProtoCodecs._
import io.iohk.atala.prism.cstore.repositories.daos.IndividualsDAO.IndividualCreateData
import io.iohk.atala.prism.cstore.repositories.IndividualsRepository
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.cstore_api.{GetHoldersRequest, GetHoldersResponse}
import io.iohk.atala.prism.protos.{cstore_api, cstore_models}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CredentialsStoreService(
    individuals: IndividualsRepository,
    storedCredentials: StoredCredentialsRepository,
    holdersRepository: ContactsRepository,
    authenticator: Authenticator
)(implicit
    ec: ExecutionContext
) extends cstore_api.CredentialsStoreServiceGrpc.CredentialsStoreService
    with ErrorSupport {

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createIndividual(
      request: cstore_api.CreateIndividualRequest
  ): Future[cstore_api.CreateIndividualResponse] = {
    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)

      val createData = IndividualCreateData(
        fullName = request.fullName,
        email = Some(request.email).filterNot(_.isEmpty)
      )

      individuals
        .createIndividual(participantId, createData)
        .wrapExceptions
        .successMap { individual =>
          cstore_api.CreateIndividualResponse(individual = Some(toIndividualProto(individual)))
        }
    }

    authenticator.authenticated("createIndividual", request) { participantId =>
      f(participantId)
    }
  }

  override def getIndividuals(request: cstore_api.GetIndividualsRequest): Future[cstore_api.GetIndividualsResponse] = {
    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)

      val lastSeen = Some(request.lastSeenIndividualId).filterNot(_.isEmpty).map(ParticipantId.apply)

      individuals
        .getIndividuals(participantId, lastSeen, request.limit)
        .wrapExceptions
        .successMap { individuals =>
          cstore_api.GetIndividualsResponse(
            individuals = individuals.map(toIndividualProto)
          )
        }
    }

    authenticator.authenticated("getIndividuals", request) { participantId =>
      f(participantId)
    }

  }

  override def createHolder(request: cstore_api.CreateHolderRequest): Future[cstore_api.CreateHolderResponse] = {
    def f(participantId: ParticipantId) = {
      Future {
        implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)
        val contactData = CreateContact(
          createdBy = Institution.Id(participantId.uuid),
          externalId = Contact.ExternalId.random(),
          data = io.circe.parser.parse(request.jsonData).getOrElse(throw new RuntimeException("Invalid json"))
        )
        holdersRepository
          .create(contactData, None)
          .wrapExceptions
          .successMap(toHolderProto)
          .map(cstore_api.CreateHolderResponse().withHolder)
      }.flatten
    }

    authenticator.authenticated("createHolder", request) { participantId =>
      f(participantId)
    }
  }

  override def getHolders(request: GetHoldersRequest): Future[GetHoldersResponse] = {
    def f(createdBy: Institution.Id) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> createdBy)

      val lastSeen =
        Some(request.lastSeenHolderId).filterNot(_.isEmpty).map(UUID.fromString _ andThen Contact.Id.apply)

      holdersRepository
        .getBy(createdBy, lastSeen, None, request.limit)
        .wrapExceptions
        .successMap { holders =>
          cstore_api.GetHoldersResponse(
            holders = holders.map(toHolderProto)
          )
        }
    }

    authenticator.authenticated("getIndividuals", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def generateConnectionTokenFor(
      request: cstore_api.GenerateConnectionTokenForRequest
  ): Future[cstore_api.GenerateConnectionTokenForResponse] = {

    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)

      for {
        individualId <- Future.fromTry(
          Try { Contact.Id(UUID.fromString(request.individualId)) }
        )
        response <-
          individuals
            .generateTokenFor(participantId, individualId)
            .wrapExceptions
            .successMap { token =>
              cstore_api.GenerateConnectionTokenForResponse(token.token)
            }
      } yield response
    }

    authenticator.authenticated("generateConnectionTokenFor", request) { participantId =>
      f(participantId)
    }
  }

  override def storeCredential(
      request: cstore_api.StoreCredentialRequest
  ): Future[cstore_api.StoreCredentialResponse] = {
    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)

      val createData = StoredSignedCredentialData(
        ConnectionId.apply(request.connectionId),
        request.encodedSignedCredential
      )

      storedCredentials
        .storeCredential(createData)
        .wrapExceptions
        .successMap { _ =>
          cstore_api.StoreCredentialResponse()
        }
    }

    authenticator.authenticated("storeCredential", request) { participantId =>
      f(participantId)
    }
  }

  override def getStoredCredentialsFor(
      request: cstore_api.GetStoredCredentialsForRequest
  ): Future[cstore_api.GetStoredCredentialsForResponse] = {
    def f(institutionId: Institution.Id) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> institutionId)

      for {
        individualId <- Future.fromTry(
          Try { Contact.Id(UUID.fromString(request.individualId)) }
        )
        response <-
          storedCredentials
            .getCredentialsFor(institutionId, individualId)
            .wrapExceptions
            .successMap { credentials =>
              cstore_api.GetStoredCredentialsForResponse(
                credentials = credentials.map { credential =>
                  cstore_models.StoredSignedCredential(
                    individualId = credential.individualId.value.toString,
                    encodedSignedCredential = credential.encodedSignedCredential,
                    storedAt = credential.storedAt.toEpochMilli
                  )
                }
              )
            }
      } yield response
    }

    authenticator.authenticated("getStoredCredentialsFor", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }
}

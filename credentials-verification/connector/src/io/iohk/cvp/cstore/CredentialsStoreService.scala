package io.iohk.cvp.cstore

import com.google.protobuf.ByteString
import io.iohk.connector.Authenticator
import io.iohk.connector.errors.{ErrorSupport, LoggingContext}
import io.iohk.cvp.cstore.models.StoreIndividual
import io.iohk.cvp.cstore.repositories.daos.IndividualsDAO.StoreIndividualCreateData
import io.iohk.cvp.cstore.repositories.daos.StoredCredentialsDAO.StoredCredentialCreateData
import io.iohk.cvp.cstore.services.{StoreIndividualsService, StoreUsersService, StoredCredentialsService}
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.protos.{cstore_api, cstore_models}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialsStoreService(
    storeUsers: StoreUsersService,
    individuals: StoreIndividualsService,
    storedCredentials: StoredCredentialsService,
    authenticator: Authenticator
)(
    implicit ec: ExecutionContext
) extends cstore_api.CredentialsStoreServiceGrpc.CredentialsStoreService
    with ErrorSupport {

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def register(request: cstore_api.RegisterRequest): Future[cstore_api.RegisterResponse] = {
    implicit val loggingContext = LoggingContext("request" -> request)
    def f() = {
      val createData = StoreUsersService.StoreUserCreationData(
        request.name,
        Option(request.logo).filter(!_.isEmpty).map(_.toByteArray.toVector)
      )

      storeUsers
        .insert(createData)
        .wrapExceptions
        .successMap { id =>
          cstore_api.RegisterResponse(userId = id.uuid.toString)
        }
    }
    authenticator.public("register", request) { f() }

  }

  override def createIndividual(
      request: cstore_api.CreateIndividualRequest
  ): Future[cstore_api.CreateIndividualResponse] = {
    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)

      val createData = StoreIndividualCreateData(
        fullName = request.fullName,
        email = Some(request.email).filterNot(_.isEmpty)
      )

      individuals
        .createIndividual(participantId, createData)
        .wrapExceptions
        .successMap { individual =>
          cstore_api.CreateIndividualResponse(individual = Some(individualModelToProto(individual)))
        }
    }

    authenticator.authenticated("createIndividual", request) { participantId =>
      f(participantId)
    }

  }

  def individualModelToProto(individual: StoreIndividual): cstore_models.Individual = {
    cstore_models.Individual(
      individualId = individual.id.uuid.toString,
      status = cstore_models.IndividualConnectionStatus
        .fromName(individual.status.entryName)
        .getOrElse(throw new Exception(s"Unknown status: ${individual.status}")),
      fullName = individual.fullName,
      connectionToken = individual.connectionToken.getOrElse(""),
      connectionId = individual.connectionId.fold("")(_.id.toString),
      email = individual.email.getOrElse("")
    )
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
            individuals = individuals.map(individualModelToProto)
          )
        }
    }

    authenticator.authenticated("getIndividuals", request) { participantId =>
      f(participantId)
    }

  }

  override def generateConnectionTokenFor(
      request: cstore_api.GenerateConnectionTokenForRequest
  ): Future[cstore_api.GenerateConnectionTokenForResponse] = {

    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)

      val individualId = ParticipantId(request.individualId)

      individuals
        .generateTokenFor(participantId, individualId)
        .wrapExceptions
        .successMap { token =>
          cstore_api.GenerateConnectionTokenForResponse(token.token)
        }
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

      val createData = StoredCredentialCreateData(
        ParticipantId.apply(request.individualId),
        request.issuerDid,
        request.proofId,
        request.content.toByteArray,
        request.signature.toByteArray
      )

      storedCredentials
        .storeCredential(participantId, createData)
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
    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)

      val individualId = ParticipantId(request.individualId)

      storedCredentials
        .getCredentialsFor(participantId, individualId)
        .wrapExceptions
        .successMap { credentials =>
          cstore_api.GetStoredCredentialsForResponse(
            credentials = credentials.map { credential =>
              cstore_models.SignedCredential(
                issuerDid = credential.issuerDid,
                proofId = credential.proofId,
                content = ByteString.copyFrom(credential.content)
              )
            }
          )
        }
    }

    authenticator.authenticated("getStoredCredentialsFor", request) { participantId =>
      f(participantId)
    }
  }

}

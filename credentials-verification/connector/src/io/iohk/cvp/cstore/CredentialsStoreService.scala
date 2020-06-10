package io.iohk.cvp.cstore

import java.util.UUID

import com.google.protobuf.ByteString
import io.iohk.connector.Authenticator
import io.iohk.connector.errors.{ErrorSupport, LoggingContext}
import io.iohk.cvp.cstore.grpc.ProtoCodecs._
import io.iohk.cvp.cstore.models.Verifier
import io.iohk.cvp.cstore.repositories.VerifierHoldersRepository
import io.iohk.cvp.cstore.repositories.daos.StoredCredentialsDAO.StoredCredentialCreateData
import io.iohk.cvp.cstore.repositories.daos.VerifierHoldersDAO.VerifierHolderCreateData
import io.iohk.cvp.cstore.services.{StoredCredentialsService, VerifierHoldersService}
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.protos.cstore_api.{GetHoldersRequest, GetHoldersResponse}
import io.iohk.prism.protos.{cstore_api, cstore_models}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialsStoreService(
    individuals: VerifierHoldersService,
    storedCredentials: StoredCredentialsService,
    holdersRepository: VerifierHoldersRepository,
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

      val createData = VerifierHolderCreateData(
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
        val json = io.circe.parser.parse(request.jsonData).getOrElse(throw new RuntimeException("Invalid json"))
        val verifierId = Verifier.Id(participantId.uuid)
        holdersRepository
          .create(verifierId, json)
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
    def f(verifierId: Verifier.Id) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> verifierId)

      val lastSeen =
        Some(request.lastSeenHolderId).filterNot(_.isEmpty).map(UUID.fromString _ andThen Verifier.Id.apply)

      holdersRepository
        .list(verifierId, lastSeen, request.limit)
        .wrapExceptions
        .successMap { holders =>
          cstore_api.GetHoldersResponse(
            holders = holders.map(toHolderProto)
          )
        }
    }

    authenticator.authenticated("getIndividuals", request) { participantId =>
      f(Verifier.Id(participantId.uuid))
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

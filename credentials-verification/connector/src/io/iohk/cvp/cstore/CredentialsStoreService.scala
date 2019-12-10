package io.iohk.cvp.cstore

import com.google.protobuf.ByteString
import io.iohk.connector.errors.{ErrorSupport, LoggingContext}
import io.iohk.cvp.cstore.models.StoreIndividual
import io.iohk.cvp.cstore.protos._
import io.iohk.cvp.cstore.repositories.daos.IndividualsDAO.StoreIndividualCreateData
import io.iohk.cvp.cstore.repositories.daos.StoredCredentialsDAO.StoredCredentialCreateData
import io.iohk.cvp.cstore.services.{StoreIndividualsService, StoredCredentialsService}
import io.iohk.cvp.grpc.UserIdInterceptor
import io.iohk.cvp.models.ParticipantId
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialsStoreService(individuals: StoreIndividualsService, storedCredentials: StoredCredentialsService)(
    implicit ec: ExecutionContext
) extends CredentialsStoreServiceGrpc.CredentialsStoreService
    with ErrorSupport {

  override def logger: Logger = LoggerFactory.getLogger(getClass)

  override def createIndividual(request: CreateIndividualRequest): Future[CreateIndividualResponse] = {
    val userId = UserIdInterceptor.participantId()
    implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

    val createData = StoreIndividualCreateData(
      fullName = request.fullName,
      email = Some(request.email).filterNot(_.isEmpty)
    )

    individuals
      .createIndividual(userId, createData)
      .wrapExceptions
      .successMap { individual =>
        protos.CreateIndividualResponse(individual = Some(individualModelToProto(individual)))
      }
  }

  def individualModelToProto(individual: StoreIndividual): protos.Individual = {
    protos.Individual(
      individualId = individual.id.uuid.toString,
      status = protos.IndividualConnectionStatus
        .fromName(individual.status.entryName)
        .getOrElse(throw new Exception(s"Unknown status: ${individual.status}")),
      fullName = individual.fullName,
      connectionToken = individual.connectionToken.getOrElse(""),
      connectionId = individual.connectionId.fold("")(_.id.toString),
      email = individual.email.getOrElse("")
    )
  }

  override def getIndividuals(request: GetIndividualsRequest): Future[GetIndividualsResponse] = {
    val userId = UserIdInterceptor.participantId()
    implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

    val lastSeen = Some(request.lastSeenIndividualId).filterNot(_.isEmpty).map(ParticipantId.apply)

    individuals
      .getIndividuals(userId, lastSeen, request.limit)
      .wrapExceptions
      .successMap { individuals =>
        protos.GetIndividualsResponse(
          individuals = individuals.map(individualModelToProto)
        )
      }
  }

  override def generateConnectionTokenFor(
      request: GenerateConnectionTokenForRequest
  ): Future[GenerateConnectionTokenForResponse] = {
    val userId = UserIdInterceptor.participantId()
    implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

    val individualId = ParticipantId(request.individualId)

    individuals
      .generateTokenFor(userId, individualId)
      .wrapExceptions
      .successMap { token =>
        protos.GenerateConnectionTokenForResponse(token.token)
      }
  }

  override def storeCredential(request: StoreCredentialRequest): Future[StoreCredentialResponse] = {
    val userId = UserIdInterceptor.participantId()
    implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

    val createData = StoredCredentialCreateData(
      ParticipantId.apply(request.individualId),
      request.issuerDid,
      request.proofId,
      request.content.toByteArray,
      request.signature.toByteArray
    )

    storedCredentials
      .storeCredential(userId, createData)
      .wrapExceptions
      .successMap { _ =>
        StoreCredentialResponse()
      }
  }

  override def getStoredCredentialsFor(
      request: GetStoredCredentialsForRequest
  ): Future[GetStoredCredentialsForResponse] = {
    val userId = UserIdInterceptor.participantId()
    implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)
    val individualId = ParticipantId(request.individualId)

    storedCredentials
      .getCredentialsFor(userId, individualId)
      .wrapExceptions
      .successMap { credentials =>
        protos.GetStoredCredentialsForResponse(
          credentials = credentials.map { credential =>
            protos.SignedCredential(
              issuerDid = credential.issuerDid,
              proofId = credential.proofId,
              content = ByteString.copyFrom(credential.content)
            )
          }
        )
      }
  }
}

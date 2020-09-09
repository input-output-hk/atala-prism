package io.iohk.atala.prism.cmanager.grpc.services

import java.util.UUID

import io.iohk.atala.prism.connector.Authenticator
import io.iohk.atala.prism.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.atala.prism.cmanager.models.requests.{
  CreateGenericCredential,
  CreateUniversityCredential,
  PublishCredential
}
import io.iohk.atala.prism.cmanager.models.{GenericCredential, Issuer, Student, Subject, UniversityCredential}
import io.iohk.atala.prism.cmanager.repositories.CredentialsRepository
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.syntax._
import io.iohk.atala.prism.protos.cmanager_api
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.cmanager_api.{
  CreateGenericCredentialRequest,
  CreateGenericCredentialResponse,
  GetGenericCredentialsRequest,
  GetGenericCredentialsResponse,
  PublishCredentialRequest,
  PublishCredentialResponse
}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CredentialsServiceImpl(
    credentialsRepository: CredentialsRepository,
    authenticator: Authenticator,
    nodeService: NodeServiceGrpc.NodeService
)(implicit
    ec: ExecutionContext
) extends cmanager_api.CredentialsServiceGrpc.CredentialsService {

  override def createCredential(
      request: cmanager_api.CreateCredentialRequest
  ): Future[cmanager_api.CreateCredentialResponse] =
    authenticatedHandler("createCredential", request) { issuerId =>
      val studentId = Student.Id(UUID.fromString(request.studentId))
      val model = request
        .into[CreateUniversityCredential]
        .withFieldConst(_.issuedBy, issuerId)
        .withFieldConst(_.studentId, studentId)
        .enableUnsafeOption
        .transform

      credentialsRepository
        .createUniversityCredential(model)
        .map(universityCredentialToProto)
        .map(cmanager_api.CreateCredentialResponse().withCredential)
    }

  override def getCredentials(
      request: cmanager_api.GetCredentialsRequest
  ): Future[cmanager_api.GetCredentialsResponse] =
    authenticatedHandler("getCredentials", request) { issuerId =>
      val lastSeenCredential =
        Try(UUID.fromString(request.lastSeenCredentialId)).map(UniversityCredential.Id.apply).toOption
      credentialsRepository
        .getUniversityCredentialsBy(issuerId, request.limit, lastSeenCredential)
        .map { list =>
          cmanager_api.GetCredentialsResponse(list.map(universityCredentialToProto))
        }
    }

  /** Generic versions
    */
  override def createGenericCredential(
      request: CreateGenericCredentialRequest
  ): Future[CreateGenericCredentialResponse] =
    authenticatedHandler("createGenericCredential", request) { issuerId =>
      val subjectId = Subject.Id(UUID.fromString(request.subjectId))
      lazy val json =
        io.circe.parser.parse(request.credentialData).getOrElse(throw new RuntimeException("Invalid json"))
      val model = request
        .into[CreateGenericCredential]
        .withFieldConst(_.issuedBy, issuerId)
        .withFieldConst(_.subjectId, subjectId)
        .withFieldConst(_.credentialData, json)
        .enableUnsafeOption
        .transform

      credentialsRepository
        .create(model)
        .map(genericCredentialToProto)
        .map(cmanager_api.CreateGenericCredentialResponse().withGenericCredential)
    }

  override def getGenericCredentials(
      request: GetGenericCredentialsRequest
  ): Future[GetGenericCredentialsResponse] =
    authenticatedHandler("getGenericCredentials", request) { issuerId =>
      val lastSeenCredential =
        Try(UUID.fromString(request.lastSeenCredentialId)).map(GenericCredential.Id.apply).toOption
      credentialsRepository
        .getBy(issuerId, request.limit, lastSeenCredential)
        .map { list =>
          cmanager_api.GetGenericCredentialsResponse(list.map(genericCredentialToProto))
        }
    }

  /** Publish an encoded signed credential into the blockchain
    */
  override def publishCredential(request: PublishCredentialRequest): Future[PublishCredentialResponse] = {
    authenticator.authenticated("publishCredential", request) { participantId =>
      for {
        issuerId <- Issuer.Id(participantId.uuid).tryF
        credentialId = GenericCredential.Id(UUID.fromString(request.cmanagerCredentialId))
        credentialProtocolId = request.nodeCredentialId
        issuanceOperationHash = SHA256Digest(request.operationHash.toByteArray)
        encodedSignedCredential = request.encodedSignedCredential
        issueCredentialOp =
          request.issueCredentialOperation
            .getOrElse(throw new RuntimeException("Missing IssueCredential operation"))
        // validation for sanity check
        _ = require(
          credentialProtocolId == issuanceOperationHash.hexValue,
          "operation hash and credential id don't match"
        )
        _ = require(encodedSignedCredential.nonEmpty, "Empty encoded credential")
        // we update the database
        _ <-
          credentialsRepository
            .storePublicationData(
              issuerId,
              PublishCredential(credentialId, issuanceOperationHash, credentialProtocolId, encodedSignedCredential)
            )
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
        // TODO: For this release we optimistically assume that the node will always manage to
        //       publish the credential
        _ <- nodeService.issueCredential {
          node_api
            .IssueCredentialRequest()
            .withSignedOperation(issueCredentialOp)
        }
      } yield cmanager_api.PublishCredentialResponse()
    }
  }

  private def authenticatedHandler[Request <: scalapb.GeneratedMessage, Response <: scalapb.GeneratedMessage](
      methodName: String,
      request: Request
  )(
      block: Issuer.Id => FutureEither[Nothing, Response]
  ): Future[Response] = {
    authenticator.authenticated(methodName, request) { participantId =>
      block(Issuer.Id(participantId.uuid)).value
        .map {
          case Right(x) => x
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
  }
}

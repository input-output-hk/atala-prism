package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs.genericCredentialToProto
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialsRepository
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.console_api
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialsServiceImpl(
    credentialsRepository: CredentialsRepository,
    val authenticator: ManagementConsoleAuthenticator,
    nodeService: NodeServiceGrpc.NodeService
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialsServiceGrpc.CredentialsService
    with ManagementConsoleErrorSupport
    with AuthSupport[ManagementConsoleError, ParticipantId] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGenericCredential(
      request: CreateGenericCredentialRequest
  ): Future[CreateGenericCredentialResponse] =
    auth[CreateGenericCredential]("createGenericCredential", request) { (participantId, query) =>
      credentialsRepository
        .create(participantId, query)
        .map(genericCredentialToProto)
        .map { created =>
          console_api.CreateGenericCredentialResponse().withGenericCredential(created)
        }
    }

  override def getGenericCredentials(
      request: GetGenericCredentialsRequest
  ): Future[GetGenericCredentialsResponse] =
    auth[GetGenericCredential]("getGenericCredentials", request) { (participantId, query) =>
      credentialsRepository
        .getBy(participantId, query.limit, query.lastSeenCredentialId)
        .map { list =>
          console_api.GetGenericCredentialsResponse(list.map(genericCredentialToProto))
        }
    }

  override def getContactCredentials(request: GetContactCredentialsRequest): Future[GetContactCredentialsResponse] =
    auth[GetContactCredentials]("getContactCredentials", request) { (participantId, query) =>
      credentialsRepository
        .getBy(participantId, query.contactId)
        .map { list =>
          console_api.GetContactCredentialsResponse(list.map(genericCredentialToProto))
        }
    }

  override def shareCredential(request: ShareCredentialRequest): Future[ShareCredentialResponse] =
    auth[ShareCredential]("shareCredential", request) { (participantId, query) =>
      credentialsRepository
        .markAsShared(participantId, query.credentialId)
        .map { _ =>
          console_api.ShareCredentialResponse()
        }
    }

  override def getBlockchainData(request: GetBlockchainDataRequest): Future[GetBlockchainDataResponse] = ???

  override def publishBatch(request: PublishBatchRequest): Future[PublishBatchResponse] = {
    println(nodeService.toString) // added only to remove "no use" error
    ???
  }

  override def revokePublishedCredential(
      request: RevokePublishedCredentialRequest
  ): Future[RevokePublishedCredentialResponse] = {
    ???
  }

  override def storePublishedCredential(
      request: StorePublishedCredentialRequest
  ): Future[StorePublishedCredentialResponse] = ???
}

package io.iohk.atala.cvp.webextension.background.services.console

import cats.syntax.functor._
import com.google.protobuf.ByteString
import io.iohk.atala.cvp.webextension.background.services.metadataForRequest
import io.iohk.atala.prism.crypto.ECKeyPair
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.console_api
import scalapb.grpc.Channels

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.JSConverters.JSRichMap

class ConsoleClientService(url: String)(implicit ec: ExecutionContext) {
  private val credentialsStoreApi = console_api.CredentialsStoreServiceGrpcWeb.stub(Channels.grpcwebChannel(url))
  private val consoleApi = console_api.ConsoleServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def registerDID(
      did: DID,
      name: String,
      logo: Array[Byte]
  ): Future[Unit] = {
    val request = console_api
      .RegisterConsoleDIDRequest()
      .withDid(did.value)
      .withName(name)
      .withLogo(ByteString.copyFrom(logo))

    consoleApi
      .registerDID(request)
      .void
  }

  def getCurrentUser(ecKeyPair: ECKeyPair, did: DID): Future[console_api.GetConsoleCurrentUserResponse] = {
    val request = console_api.GetConsoleCurrentUserRequest()
    val metadata: Map[String, String] = metadataForRequest(ecKeyPair, did, request)
    consoleApi.getCurrentUser(request, metadata.toJSDictionary)
  }

  def getLatestCredentialExternalId(
      ecKeyPair: ECKeyPair,
      did: DID
  ): Future[console_api.GetLatestCredentialExternalIdResponse] = {
    val request = console_api.GetLatestCredentialExternalIdRequest()
    val metadata = metadataForRequest(ecKeyPair, did, request)
    credentialsStoreApi.getLatestCredentialExternalId(request, metadata.toJSDictionary)
  }

  def storeCredentials(
      ecKeyPair: ECKeyPair,
      did: DID,
      request: console_api.StoreCredentialRequest
  ): Future[console_api.StoreCredentialResponse] = {
    val metadata = metadataForRequest(ecKeyPair, did, request)
    credentialsStoreApi.storeCredential(request, metadata.toJSDictionary)
  }
}

object ConsoleClientService {
  def apply(url: String)(implicit ec: ExecutionContext): ConsoleClientService = new ConsoleClientService(url)
}

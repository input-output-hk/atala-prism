package io.iohk.atala.cvp.webextension.background.services.console

import io.iohk.atala.cvp.webextension.background.services.metadataForRequest
import io.iohk.atala.prism.crypto.ECKeyPair
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.cstore_api
import scalapb.grpc.Channels

import scala.concurrent.Future
import scala.scalajs.js.JSConverters.JSRichMap

class ConsoleClientService(url: String) {
  private val consoleApi = cstore_api.CredentialsStoreServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def getLatestCredentialExternalId(
      ecKeyPair: ECKeyPair,
      did: DID
  ): Future[cstore_api.GetLatestCredentialExternalIdResponse] = {
    val request = cstore_api.GetLatestCredentialExternalIdRequest()
    val metadata = metadataForRequest(ecKeyPair, did, request)
    consoleApi.getLatestCredentialExternalId(request, metadata.toJSDictionary)
  }

  def storeCredentials(
      ecKeyPair: ECKeyPair,
      did: DID,
      request: cstore_api.StoreCredentialRequest
  ): Future[cstore_api.StoreCredentialResponse] = {
    val metadata = metadataForRequest(ecKeyPair, did, request)
    consoleApi.storeCredential(request, metadata.toJSDictionary)
  }
}

object ConsoleClientService {
  def apply(url: String): ConsoleClientService = new ConsoleClientService(url)
}

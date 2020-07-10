package io.iohk.atala.cvp.webextension.background.services.connector

import java.util.{Base64, UUID}

import io.iohk.atala.crypto.{EC, ECKeyPair, ECPrivateKey}
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService._
import io.iohk.atala.cvp.webextension.common.ECKeyOperation
import io.iohk.atala.cvp.webextension.common.ECKeyOperation._
import io.iohk.prism.protos.connector_api
import io.iohk.prism.protos.connector_api.{
  GetCurrentUserRequest,
  GetCurrentUserResponse,
  RegisterDIDRequest,
  RegisterDIDResponse
}
import scalapb.grpc.Channels

import scala.concurrent.Future
import scala.scalajs.js.JSConverters._

class ConnectorClientService(url: String) {
  val connectorApi = connector_api.ConnectorServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def registerDID(request: RegisterDIDRequest): Future[RegisterDIDResponse] = {
    connectorApi.registerDID(request)
  }

  def getCurrentUser(ecKeyPair: ECKeyPair): Future[GetCurrentUserResponse] = {
    val requestNonce = GetRequestNonce()
    val request = GetCurrentUserRequest()
    val requestWithNonce = mergeBytes(requestNonce, request)

    val did = "did" -> didFromMasterKey(ecKeyPair)
    val didKeyId = "didKeyId" -> ECKeyOperation.firstMasterKeyId
    val didSignature = "didSignature" -> generateUrlEncodedSignature(requestWithNonce, ecKeyPair.privateKey)
    val requestNoncePair = "requestNonce" -> getUrlEncodedRequestNonce(requestNonce)
    val metadata = Map(did, didKeyId, didSignature, requestNoncePair)

    connectorApi.getCurrentUser(request, metadata.toJSDictionary)
  }
}

object ConnectorClientService {
  case class RequestNonce(bytes: Array[Byte]) extends AnyVal

  def apply(url: String): ConnectorClientService = new ConnectorClientService(url)

  private def mergeBytes(requestNonce: RequestNonce, request: GetCurrentUserRequest) = {
    requestNonce.bytes ++ request.toByteArray
  }

  private def generateUrlEncodedSignature(data: Array[Byte], privateKey: ECPrivateKey): String = {
    val signature = EC.sign(data, privateKey)
    Base64.getUrlEncoder.encodeToString(signature.data)
  }

  private def getUrlEncodedRequestNonce(requestNonce: RequestNonce): String = {
    Base64.getUrlEncoder.encodeToString(requestNonce.bytes)
  }

  private def GetRequestNonce(): RequestNonce = {
    RequestNonce(bytes = UUID.randomUUID().toString.getBytes)
  }
}

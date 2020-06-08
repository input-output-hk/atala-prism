package io.iohk.atala.cvp.webextension.background.services.connector

import java.util.{Base64, UUID}

import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService._
import io.iohk.atala.cvp.webextension.common.ECKeyOperation._
import io.iohk.atala.cvp.webextension.common.{ECKeyOperation, EcKeyPair}
import io.iohk.prism.protos.connector_api
import io.iohk.prism.protos.connector_api.{
  GetCurrentUserRequest,
  GetCurrentUserResponse,
  RegisterDIDRequest,
  RegisterDIDResponse
}
import scalapb.grpc.Channels
import typings.elliptic.mod.ec.Signature
import typings.hashJs.{hashJsStrings, mod => hash}

import scala.concurrent.Future
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.{Int8Array, Uint8Array}

class ConnectorClientService(url: String) {
  val connectorApi = connector_api.ConnectorServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def registerDID(request: RegisterDIDRequest): Future[RegisterDIDResponse] = {
    connectorApi.registerDID(request)
  }

  def getCurrentUser(ecKeyPair: EcKeyPair): Future[GetCurrentUserResponse] = {
    val requestNonce = GetRequestNonce()
    val request = GetCurrentUserRequest()
    val sha256Hashed = sha256(mergeBytes(requestNonce, request))

    val did = "did" -> didFromMasterKey(ecKeyPair)
    val didKeyId = "didKeyId" -> ECKeyOperation.firstMasterKeyId
    val didSignature = "didSignature" -> getUrlEncodedSignature(sha256Hashed, ecKeyPair)
    val requestNoncePair = "requestNonce" -> getUrlEncodedRequestNonce(requestNonce)
    val metadata = Map(did, didKeyId, didSignature, requestNoncePair)

    connectorApi.getCurrentUser(request, metadata.toJSDictionary)
  }
}

object ConnectorClientService {
  case class RequestNonce(bytes: Array[Byte]) extends AnyVal

  def apply(url: String): ConnectorClientService = new ConnectorClientService(url)

  def sha256(bytes: Array[Byte]): String = {
    val sha256 = hash.sha256().update(bytes.toJSArray.asInstanceOf[Uint8Array])
    sha256.digest_hex(hashJsStrings.hex)
  }

  def mergeBytes(requestNonce: RequestNonce, request: GetCurrentUserRequest) = {
    requestNonce.bytes ++ request.toByteArray
  }

  def getUrlEncodedSignature(sha256Hashed: String, ecKeyPair: EcKeyPair): String = {
    val signature: Signature = ecKeyPair.privateKeyPair.sign(sha256Hashed)
    Base64.getUrlEncoder.encodeToString(signature.toDER().asInstanceOf[Int8Array].toArray)
  }

  def getUrlEncodedRequestNonce(requestNonce: RequestNonce): String = {
    Base64.getUrlEncoder.encodeToString(requestNonce.bytes)
  }

  def GetRequestNonce(): RequestNonce = {
    RequestNonce(bytes = UUID.randomUUID().toString.getBytes)
  }
}

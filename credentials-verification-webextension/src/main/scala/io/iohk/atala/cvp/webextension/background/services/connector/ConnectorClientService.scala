package io.iohk.atala.cvp.webextension.background.services.connector

import scalapb.grpc.Channels
import io.iohk.prism.protos.connector_api
import io.iohk.prism.protos.connector_api.{
  GetCurrentUserRequest,
  GetCurrentUserResponse,
  RegisterDIDRequest,
  RegisterDIDResponse
}
import scalapb.grpcweb.Metadata

import scala.concurrent.Future
class ConnectorClientService(url: String) {
  val connectorApi = connector_api.ConnectorServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def registerDID(request: RegisterDIDRequest): Future[RegisterDIDResponse] = {
    connectorApi.registerDID(request)
  }

  def getCurrentUser(request: GetCurrentUserRequest, metadata: Metadata): Future[GetCurrentUserResponse] = {
    connectorApi.getCurrentUser(request, metadata)
  }
}

object ConnectorClientService {
  def apply(url: String): ConnectorClientService = new ConnectorClientService(url)
}

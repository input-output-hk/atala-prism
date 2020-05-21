package io.iohk.atala.cvp.webextension.background.services.connector

import scalapb.grpc.Channels
import io.iohk.prism.protos.connector_api
import io.iohk.prism.protos.connector_api.{RegisterDIDRequest, RegisterDIDResponse}

import scala.concurrent.Future
class ConnectorClientService(url: String) {
  val connectorApi = connector_api.ConnectorServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def registerDID(request: RegisterDIDRequest): Future[RegisterDIDResponse] = {
    connectorApi.registerDID(request)
  }
}

object ConnectorClientService {
  def apply(url: String): ConnectorClientService = new ConnectorClientService(url)
}

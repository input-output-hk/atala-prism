package io.iohk.atala.cvp.webextension.testing

import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService
import io.iohk.prism.protos.connector_api.{RegisterDIDRequest, RegisterDIDResponse}
import scala.concurrent.Future

object FakeConnectorClientService extends ConnectorClientService(url = "http://loclahost:10000/test") {
  override def registerDID(request: RegisterDIDRequest): Future[RegisterDIDResponse] = {
    Future.successful(RegisterDIDResponse(did = "testDid"))
  }
}

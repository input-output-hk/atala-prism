package io.iohk.atala.cvp.webextension.testing

import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService
import io.iohk.atala.cvp.webextension.common.models.Role
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.connector_api.RegisterDIDResponse
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

import scala.concurrent.Future

object FakeConnectorClientService extends ConnectorClientService(url = "http://loclahost:10000/test") {
  override def registerDID(
      operation: SignedAtalaOperation,
      name: String,
      logo: Array[Byte],
      role: Role
  ): Future[RegisterDIDResponse] = {
    Future.successful(RegisterDIDResponse(did = DID.buildPrismDID("test").value))
  }
}

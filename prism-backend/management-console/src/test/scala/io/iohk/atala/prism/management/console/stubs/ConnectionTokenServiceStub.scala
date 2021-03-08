package io.iohk.atala.prism.management.console.stubs

import io.iohk.atala.prism.management.console.integrations.ConnectionTokenService
import io.iohk.atala.prism.management.console.models.GenerateConnectionTokenRequestMetadata
import io.iohk.atala.prism.protos.connector_api.GenerateConnectionTokenResponse

import scala.concurrent.Future

class ConnectionTokenServiceStub extends ConnectionTokenService {
  def generateConnectionTokens(
      metadata: GenerateConnectionTokenRequestMetadata,
      count: Int
  ): Future[GenerateConnectionTokenResponse] = {
    val tokens = 1.to(count).map(i => s"ConnectionToken$i")
    Future.successful(GenerateConnectionTokenResponse(tokens))
  }
}

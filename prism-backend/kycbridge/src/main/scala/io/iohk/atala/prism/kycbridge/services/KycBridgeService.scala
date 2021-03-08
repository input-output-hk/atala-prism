package io.iohk.atala.prism.kycbridge.services

import monix.eval.Task
import doobie.util.transactor.Transactor
import io.iohk.atala.kycbridge.protos.kycbridge_api.CreateAccountResponse
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.models.{ConnectionState, ConnectionToken}
import io.iohk.atala.prism.services.ConnectorClientService
import io.iohk.atala.prism.kycbridge.models.Connection
import doobie.implicits._

class KycBridgeService(tx: Transactor[Task], connectorService: ConnectorClientService) {

  def createAccount: Task[CreateAccountResponse] = {
    connectorService.generateConnectionToken
      .flatMap(response => {
        response.tokens.headOption
          .map { tokenString =>
            val newToken = ConnectionToken(tokenString)

            ConnectionDao
              .insert(
                Connection(
                  token = newToken,
                  id = None,
                  state = ConnectionState.Invited,
                  acuantDocumentInstanceId = None,
                  acuantDocumentStatus = None
                )
              )
              .transact(tx)
              .map(_ => CreateAccountResponse(newToken.token))
          }
          .getOrElse(
            Task.raiseError(
              new RuntimeException("GenerateConnectionToken response returned empty list of connection tokens")
            )
          )
      })
  }
}

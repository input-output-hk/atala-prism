package io.iohk.atala.prism.kycbridge.services

import cats.syntax.functor._
import monix.eval.Task
import doobie.util.transactor.Transactor
import io.iohk.atala.kycbridge.protos.kycbridge_api.CreateAccountResponse
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.models.{ConnectionState, ConnectionToken}
import io.iohk.atala.prism.services.ConnectorClientService
import io.iohk.atala.prism.kycbridge.models.Connection
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import doobie.implicits._
import org.slf4j.{Logger, LoggerFactory}

class KycBridgeService(tx: Transactor[Task], connectorService: ConnectorClientService) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

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
              .logSQLErrors("creating account", logger)
              .transact(tx)
              .as(CreateAccountResponse(newToken.token))
          }
          .getOrElse(
            Task.raiseError(
              new RuntimeException("GenerateConnectionToken response returned empty list of connection tokens")
            )
          )
      })
  }
}

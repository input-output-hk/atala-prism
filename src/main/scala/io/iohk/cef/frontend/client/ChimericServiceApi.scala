package io.iohk.cef.frontend.client

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import io.iohk.cef.LedgerId
import io.iohk.cef.frontend.models.{
  CreateChimericTransactionRequest,
  SubmitChimericTransactionFragment,
  SubmitChimericTransactionRequest
}
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.chimeric.ChimericTx

import scala.concurrent.ExecutionContext

class ChimericServiceApi(service: ChimericTransactionService)(implicit ec: ExecutionContext) extends Directives {

  import ChimericServiceApi._

  def create: Route = {
    path("chimeric-transactions") {
      post {
        entity(as[CreateChimericTransactionRequest]) { request =>
          val responseHandler =
            for {
              ct <- service.createChimericTransaction(request).onFor
              res <- service.submitChimericTransaction(toSubmitChimericTransactionRequest(ct, request.ledgerId)).onFor
            } yield res

          onSuccess(responseHandler.res) { response =>
            complete(
              response match {
                case Right(_) => StatusCodes.Created
                case Left(_) => StatusCodes.BadRequest
              }
            )
          }
        }
      }
    }
  }
}

object ChimericServiceApi {

  private def toSubmitChimericTransactionRequest(
      ct: ChimericTx,
      ledgerId: LedgerId): SubmitChimericTransactionRequest = {
    val fragments = ct.fragments.map { txFragment =>
      SubmitChimericTransactionFragment(
        fragment = txFragment
      )
    }
    SubmitChimericTransactionRequest(fragments = fragments, ledgerId = ledgerId)
  }

}

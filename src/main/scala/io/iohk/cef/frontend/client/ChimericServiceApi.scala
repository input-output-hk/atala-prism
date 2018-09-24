package io.iohk.cef.frontend.client

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import io.iohk.cef.frontend.models.ChimericTransactionRequest
import io.iohk.cef.frontend.services.ChimericTransactionService

import scala.concurrent.ExecutionContext

class ChimericServiceApi(service: ChimericTransactionService)(implicit ec: ExecutionContext) extends Directives {

  import ChimericTransactionRequest._

  def create: Route = {
    path("chimeric-transactions") {
      post {
        entity(as[ChimericTransactionRequest]) { request =>
          val responseHandler = service.process(request)

          onSuccess(responseHandler) { response =>
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

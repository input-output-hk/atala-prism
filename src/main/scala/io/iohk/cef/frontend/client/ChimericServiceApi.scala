package io.iohk.cef.frontend.client

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import io.iohk.cef.frontend.models.ChimericTransactionRequest
import io.iohk.cef.frontend.services.ChimericTransactionService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationDouble

class ChimericServiceApi(service: ChimericTransactionService)(implicit ec: ExecutionContext) extends Directives {

  import ChimericTransactionRequest._

  private implicit val timeout = Timeout(2.seconds)

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

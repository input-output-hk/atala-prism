package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.alexitc.playsonify.models.{ErrorId, ServerError}
import io.iohk.cef.LedgerId
import io.iohk.cef.frontend.client.ServiceResponseExtensions
import io.iohk.cef.frontend.controllers.common._
import io.iohk.cef.frontend.models.{
  CreateChimericTransactionRequest,
  SubmitChimericTransactionFragment,
  SubmitChimericTransactionRequest
}
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.chimeric.ChimericTx

import scala.concurrent.ExecutionContext

class ChimericTransactionsController(service: ChimericTransactionService)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends CustomJsonController {

  import ChimericTransactionsController._
  import Codecs._
  import Context._

  lazy val routes: Route = {
    path("chimeric-transactions") {
      post {
        publicInput(StatusCodes.Created) { ctx: HasModel[CreateChimericTransactionRequest] =>
          val result = for {
            createResult <- service.createChimericTransaction(ctx.model).onFor

            submitRequest = toSubmitRequest(createResult, ctx.model.ledgerId)
            _ <- service.submitChimericTransaction(submitRequest).onFor
          } yield createResult

          // The actual method call never fails but the type system says it could, we need this to be able to compile
          fromFutureEither(result.res, ChimericTransactionCreationError())
        }
      }
    }
  }
}

object ChimericTransactionsController {

  final case class ChimericTransactionCreationError(id: ErrorId = ErrorId.create) extends ServerError {

    override def cause: Option[Throwable] = None

  }

  private def toSubmitRequest(ct: ChimericTx, ledgerId: LedgerId): SubmitChimericTransactionRequest = {

    val fragments = ct.fragments
      .map(SubmitChimericTransactionFragment.apply)

    SubmitChimericTransactionRequest(fragments = fragments, ledgerId = ledgerId)
  }
}

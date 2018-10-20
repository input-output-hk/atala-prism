package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.models.{ErrorId, ServerError, ApplicationError => PlaysonifyError}
import io.iohk.cef.LedgerId
import io.iohk.cef.error.{ApplicationError => CefError}
import io.iohk.cef.frontend.client
import io.iohk.cef.frontend.client.ServiceResponseExtensions
import io.iohk.cef.frontend.controllers.common.{Codecs, CustomJsonController}
import io.iohk.cef.frontend.models.{
  CreateChimericTransactionRequest,
  SubmitChimericTransactionFragment,
  SubmitChimericTransactionRequest
}
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.chimeric.ChimericTx
import org.scalactic.{Bad, Good}

import scala.concurrent.ExecutionContext

class ChimericTransactionsController(service: ChimericTransactionService)(
    implicit ec: ExecutionContext,
    mat: Materializer)
    extends CustomJsonController {

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

          fromFutureEither(result.res)
        }
      }
    }
  }
}

object ChimericTransactionsController {

  final case class ChimericTransactionCreationError(id: ErrorId = ErrorId.create) extends ServerError {

    override def cause: Option[Throwable] = None

  }

  private def fromFutureEither(value: client.Response[ChimericTx])(
      implicit ec: ExecutionContext): FutureApplicationResult[ChimericTx] = {

    value.map {
      case Left(e) => Bad(fromCefError(e)).accumulating
      case Right(result) => Good(result)
    }
  }

  private def fromCefError(error: CefError): PlaysonifyError = {
    // The actual method call never fails but the type system says it could, we need this to be able to compile
    ChimericTransactionCreationError()
  }

  private def toSubmitRequest(ct: ChimericTx, ledgerId: LedgerId): SubmitChimericTransactionRequest = {

    val fragments = ct.fragments
      .map(SubmitChimericTransactionFragment.apply)

    SubmitChimericTransactionRequest(fragments = fragments, ledgerId = ledgerId)
  }
}

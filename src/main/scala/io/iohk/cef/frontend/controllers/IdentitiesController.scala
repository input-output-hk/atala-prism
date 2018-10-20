package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.models.{ErrorId, ServerError, ApplicationError => PlaysonifyError}
import io.iohk.cef.LedgerId
import io.iohk.cef.error.{ApplicationError => CefError}
import io.iohk.cef.frontend.client
import io.iohk.cef.frontend.client.ServiceResponseExtensions
import io.iohk.cef.frontend.controllers.common.{Codecs, CustomJsonController}
import io.iohk.cef.frontend.models.{
  CreateIdentityTransactionRequest,
  IdentityTransactionType,
  SubmitIdentityTransactionRequest
}
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity.IdentityTransaction
import org.scalactic.{Bad, Good}

import scala.concurrent.ExecutionContext

class IdentitiesController(service: IdentityTransactionService)(implicit mat: Materializer, ec: ExecutionContext)
    extends CustomJsonController {

  import Codecs._
  import Context._
  import IdentitiesController._

  lazy val routes = {
    path("identities") {
      post {
        publicInput(StatusCodes.Created) { ctx: HasModel[CreateIdentityTransactionRequest] =>
          val result = for {
            tx <- service.createIdentityTransaction(ctx.model).onFor

            submitTransaction = toSubmitRequest(tx, ctx.model.ledgerId)
            _ <- service.submitIdentityTransaction(submitTransaction).onFor
          } yield tx

          fromFutureEither(result.res)
        }
      }
    }
  }
}

object IdentitiesController {

  final case class IdentityTransactionCreationError(id: ErrorId = ErrorId.create) extends ServerError {

    override def cause: Option[Throwable] = None

  }

  private def fromFutureEither(value: client.Response[IdentityTransaction])(
      implicit ec: ExecutionContext): FutureApplicationResult[IdentityTransaction] = {

    value.map {
      case Left(e) => Bad(fromCefError(e)).accumulating
      case Right(result) => Good(result)
    }
  }

  private def fromCefError(error: CefError): PlaysonifyError = {
    // The actual method call never fails but the type system says it could, we need this to be able to compile
    IdentityTransactionCreationError()
  }

  private def toSubmitRequest(it: IdentityTransaction, ledgerId: LedgerId): SubmitIdentityTransactionRequest = {

    SubmitIdentityTransactionRequest(
      `type` = IdentityTransactionType.of(it),
      identity = it.identity,
      ledgerId = ledgerId,
      publicKey = it.key,
      signature = it.signature)
  }
}

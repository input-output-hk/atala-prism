package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
import com.alexitc.playsonify.models.{ErrorId, ServerError}
import io.iohk.cef.LedgerId
import io.iohk.cef.frontend.client.ServiceResponseExtensions
import io.iohk.cef.frontend.controllers.common._
import io.iohk.cef.frontend.models.{
  CreateIdentityTransactionRequest,
  IdentityTransactionType,
  SubmitIdentityTransactionRequest
}
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity.{IdentityTransaction, Link}

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

          // The actual method call never fails but the type system says it could, we need this to be able to compile
          fromFutureEither(result.res, IdentityTransactionCreationError())
        }
      }
    }
  }
}

object IdentitiesController {

  final case class IdentityTransactionCreationError(id: ErrorId = ErrorId.create) extends ServerError {

    override def cause: Option[Throwable] = None

  }

  private def toSubmitRequest(it: IdentityTransaction, ledgerId: LedgerId): SubmitIdentityTransactionRequest = {

    val secondSignatureMaybe = it match {
      case x: Link => Option(x.linkingIdentitySignature)
      case _ => None
    }

    SubmitIdentityTransactionRequest(
      `type` = IdentityTransactionType.of(it),
      data = it.data,
      ledgerId = ledgerId,
      signature = it.signature,
      linkingIdentitySignature = secondSignatureMaybe
    )
  }
}

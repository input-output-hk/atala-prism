package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models._
import io.iohk.cef.frontend.client.ServiceResponseExtensions
import io.iohk.cef.frontend.controllers.common._
import io.iohk.cef.frontend.models.{
  CreateChimericTransactionRequest,
  SubmitChimericTransactionFragment,
  SubmitChimericTransactionRequest
}
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.ledger.chimeric.{Address, ChimericTx, TxOutRef}
import org.scalactic.{Bad, Every, Good}
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class ChimericTransactionsController(service: ChimericTransactionService)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends CustomJsonController {

  import ChimericTransactionsController._
  import Codecs._
  import Context._

  lazy val routes: Route = {
    pathPrefix("chimeric-transactions" / "currencies") {
      pathEnd {
        get {
          public { _ =>
            service.queryAllCurrencies().map {
              case Right(x) => Good(Json.obj("data" -> x))
              // The method never returns a Left, this is required to compile
              case Left(_) => Bad(Every(QueryCreatedCurrencyError))
            }
          }
        }
      } ~
        path(Segment) { currency =>
          get {
            public { _ =>
              service.queryCreatedCurrency(currency).map {
                case Right(Some(c)) => Good(Json.toJson(c))
                case Right(None) => Bad(Every(CurrencyNotFound(currency)))
                case Left(_) => Bad(Every(QueryCreatedCurrencyError))
              }
            }
          }
        }
    } ~
      path("chimeric-transactions" / "utxos" / Segment / "balance") { txOutRefCandidate =>
        get {
          public { _ =>
            TxOutRef.parse(txOutRefCandidate) match {
              case None => Future.successful(Bad(Every(InvalidTxOutRef(txOutRefCandidate))))
              case Some(txOutRef) =>
                service.queryUtxoBalance(txOutRef).map {
                  case Right(Some(response)) => Good(response)
                  case Right(None) => Bad(Every(TxOutRefNotFound(txOutRef)))
                  case Left(_) => Bad(Every(QueryUtxoBalanceError))
                }
            }
          }
        }
      } ~
      path("chimeric-transactions" / "addresses" / Segment / "balance") { address =>
        get {
          public { _ =>
            service.queryAddressBalance(address).map {
              case Right(Some(response)) => Good(response)
              case Right(None) => Bad(Every(AddressNotFound(address)))
              case Left(_) => Bad(Every(QueryAddressBalanceError))
            }
          }
        }
      } ~
      path("chimeric-transactions" / "addresses" / Segment / "nonce") { address =>
        get {
          public { _ =>
            service.queryAddressNonce(address).map {
              case Right(Some(response)) => Good(Json.toJson(response))
              case Right(None) => Bad(Every(AddressNotFound(address)))
              case Left(_) => Bad(Every(QueryAddressNonceError))
            }
          }
        }
      } ~
      path("chimeric-transactions") {
        post {
          publicInput(StatusCodes.Created) { ctx: HasModel[CreateChimericTransactionRequest] =>
            val result = for {
              createResult <- service.createChimericTransaction(ctx.model).onFor

              submitRequest = toSubmitRequest(createResult, ctx.model.ledgerId)
              _ <- service.submitChimericTransaction(submitRequest).onFor
            } yield createResult

            // The actual method call never fails but the type system says it could, we need this to be able to compile
            fromFutureEither(result.res, ChimericTransactionCreationError)
          }
        }
      }
  }
}

object ChimericTransactionsController {

  trait SimpleInternalServerError extends ServerError {
    override val id: ErrorId = ErrorId.create
    override def cause: Option[Throwable] = None
  }

  final case object ChimericTransactionCreationError extends SimpleInternalServerError
  final case object QueryCreatedCurrencyError extends SimpleInternalServerError
  final case object QueryUtxoBalanceError extends SimpleInternalServerError
  final case object QueryAddressBalanceError extends SimpleInternalServerError
  final case object QueryAddressNonceError extends SimpleInternalServerError

  private def notFound(field: String): List[PublicError] = {
    val message = s"No results found"
    List(FieldValidationError(field, message))
  }

  final case class CurrencyNotFound(missingCurrency: String) extends NotFoundError {
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] =
      notFound("currency")
  }

  final case class TxOutRefNotFound(missingTxOutRef: TxOutRef) extends NotFoundError {
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] =
      notFound("utxo")
  }

  final case class AddressNotFound(missingAddress: Address) extends NotFoundError {
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] =
      notFound("address")
  }

  final case class InvalidTxOutRef(txOutRefCandidate: String) extends InputValidationError {
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val error = FieldValidationError("txOutRef", "The txOutRef should be formatted as 'transactionID(index)'")
      List(error)
    }
  }

  private def toSubmitRequest(ct: ChimericTx, ledgerId: LedgerId): SubmitChimericTransactionRequest = {

    val fragments = ct.fragments
      .map(SubmitChimericTransactionFragment.apply)

    SubmitChimericTransactionRequest(fragments = fragments, ledgerId = ledgerId)
  }

}

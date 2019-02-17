package io.iohk.cef.frontend.controllers

import java.util.Base64

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.ByteString
import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models._
import io.iohk.crypto._
import io.iohk.cef.frontend.client.ServiceResponseExtensions
import io.iohk.cef.frontend.controllers.common._
import io.iohk.cef.frontend.models.{
  CreateChimericTransactionRequest,
  SubmitChimericTransactionFragment,
  SubmitChimericTransactionRequest,
  UnsupportedLedgerIdError
}
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.ledger.chimeric.{Address, ChimericTx, TxOutRef}
import io.iohk.cef.ledger.query.chimeric.ChimericQuery
import org.scalactic.{Bad, Every, Good, Or}
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class ChimericTransactionsController(service: ChimericTransactionService)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends CustomJsonController {

  import ChimericTransactionsController._
  import Codecs._
  import Context._

  private def applyValiations[A](
      ledgerId: LedgerId,
      result: => Future[A Or Every[ApplicationError]]
  ): Future[A Or Every[ApplicationError]] = {
    if (!service.isLedgerSupported(ledgerId))
      Future.successful(Bad(Every(UnsupportedLedgerIdError(ledgerId))))
    else
      result
  }

  lazy val routes: Route = corsHandler {
    pathPrefix("chimeric-transactions" / "currencies") {
      pathEnd {
        get {
          public { _ =>
            //replace with query string
            def ledgerId = service.ledgerId
            applyValiations(
              ledgerId,
              service.executeQuery(ledgerId, ChimericQuery.AllCurrencies).map {
                case Right(x) => Good(Json.obj("data" -> x))
                // The method never returns a Left, this is required to compile
                case Left(_) => Bad(Every(QueryCreatedCurrencyError))
              }
            )
          }
        }
      } ~
        path(Segment) { currency =>
          get {
            public { _ =>
              //replace with query string
              def ledgerId = service.ledgerId
              applyValiations(
                ledgerId,
                service.executeQuery(ledgerId, ChimericQuery.CreatedCurrency(currency)).map {
                  case Right(Some(c)) => Good(Json.toJson(c))
                  case Right(None) => Bad(Every(CurrencyNotFound(currency)))
                  case Left(_) => Bad(Every(QueryCreatedCurrencyError))
                }
              )
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
                //replace with query string
                def ledgerId = service.ledgerId
                applyValiations(
                  ledgerId,
                  service.executeQuery(ledgerId, ChimericQuery.UtxoBalance(txOutRef)).map {
                    case Right(Some(response)) => Good(response)
                    case Right(None) => Bad(Every(TxOutRefNotFound(txOutRef)))
                    case Left(_) => Bad(Every(QueryUtxoBalanceError))
                  }
                )
            }
          }
        }
      } ~
      path("chimeric-transactions" / "addresses" / Segment / "balance") { addressStr =>
        get {
          public { _ =>
            decodeAddress(addressStr)
              .map { address =>
                //replace with query string
                def ledgerId = service.ledgerId
                applyValiations(
                  ledgerId,
                  service.executeQuery(ledgerId, ChimericQuery.AddressBalance(address)).map {
                    case Right(Some(response)) => Good(response)
                    case Right(None) => Bad(Every(AddressNotFound(address)))
                    case Left(_) => Bad(Every(QueryAddressBalanceError))
                  }
                )
              }
              .getOrElse(Future.successful(Bad(Every(InvalidAddress(addressStr)))))
          }
        }
      } ~
      path("chimeric-transactions" / "addresses" / Segment / "nonce") { addressStr =>
        get {
          public { _ =>
            decodeAddress(addressStr)
              .map { address =>
                //replace with query string
                def ledgerId = service.ledgerId
                applyValiations(
                  ledgerId,
                  service.executeQuery(ledgerId, ChimericQuery.AddressNonce(address)).map {
                    case Right(Some(response)) => Good(Json.toJson(response))
                    case Right(None) => Bad(Every(AddressNotFound(address)))
                    case Left(_) => Bad(Every(QueryAddressNonceError))
                  }
                )
              }
              .getOrElse(Future.successful(Bad(Every(InvalidAddress(addressStr)))))
          }
        }
      } ~
      path("chimeric-transactions") {
        post {
          publicInput(StatusCodes.Created) { ctx: HasModel[CreateChimericTransactionRequest] =>
            //replace with query string
            def ledgerId = service.ledgerId
            def result =
              for {
                createResult <- service.createChimericTransaction(ctx.model).onFor

                submitRequest = toSubmitRequest(createResult, ctx.model.ledgerId)
                _ <- service.submitChimericTransaction(submitRequest).onFor
              } yield createResult

            // The actual method call never fails but the type system says it could, we need this to be able to compile
            applyValiations(ledgerId, fromFutureEither(result.res, ChimericTransactionCreationError))
          }
        }
      }
  }

  private def decodeAddress(address: String) = {
    SigningPublicKey.decodeFrom(ByteString(Base64.getUrlDecoder().decode(address)))
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

  final case class InvalidAddress(addressCandidate: String) extends InputValidationError {
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val error = FieldValidationError("address", "The address should be formatted as a valid signing public key.")
      List(error)
    }
  }

  private def toSubmitRequest(ct: ChimericTx, ledgerId: LedgerId): SubmitChimericTransactionRequest = {

    val fragments = ct.fragments
      .map(SubmitChimericTransactionFragment.apply)

    SubmitChimericTransactionRequest(fragments = fragments, ledgerId = ledgerId)
  }
}

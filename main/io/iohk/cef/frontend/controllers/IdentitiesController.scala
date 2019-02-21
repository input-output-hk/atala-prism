package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
import com.alexitc.playsonify.core.FutureOr.Implicits._
import com.alexitc.playsonify.models.{ErrorId, ServerError}
import io.iohk.cef.frontend.client.ServiceResponseExtensions
import io.iohk.cef.frontend.controllers.common.Codecs._
import io.iohk.cef.frontend.controllers.common._
import io.iohk.cef.frontend.models.{
  CreateIdentityTransactionRequest,
  IdentityTransactionType,
  SubmitIdentityTransactionRequest
}
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.ledger.identity.{Grant, IdentityTransaction, Link, LinkCertificate}
import io.iohk.cef.ledger.query.identity.IdentityQuery
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class IdentitiesController(service: IdentityTransactionService)(
    implicit mat: Materializer,
    ec: ExecutionContext
) extends CustomJsonController
    with LedgerController {

  import Context._
  import IdentitiesController._

  lazy val routes = corsHandler {
    pathPrefix("identities") {
      (get & pathPrefix(Segment)) { identity =>
        path("exists") {
          public { _ =>
            def queryResult = {
              val query = IdentityQuery.ExistsIdentity(identity)
              val exists = service.executeQuery(service.ledgerId, query)
              val result = exists.map(x => x.map(exists => Json.obj("exists" -> JsBoolean(exists))))
              fromFutureEither(result, IdentityQueryExecutionException())
            }
            //FIXME replace with query string
            def ledgerId = service.ledgerId
            val result = for {
              _ <- Future.successful(applyLedgerValidation(ledgerId, service)).toFutureOr
              x <- queryResult.toFutureOr
            } yield x
            result.future
          }
        } ~
          path("endorsers") {
            public { _ =>
              def queryResult = {
                val query = IdentityQuery.RetrieveEndorsers(identity)
                fromFutureEither(service.executeQuery(service.ledgerId, query), IdentityQueryExecutionException())
              }
              //FIXME replace with query string
              def ledgerId = service.ledgerId
              val result = for {
                _ <- Future.successful(applyLedgerValidation(ledgerId, service)).toFutureOr
                x <- queryResult.toFutureOr
              } yield x
              result.future
            }
          } ~
          path("endorsements") {
            public { _ =>
              def queryResult = {
                val query = IdentityQuery.RetrieveEndorsements(identity)
                val result = service.executeQuery(service.ledgerId, query)
                fromFutureEither(result, IdentityQueryExecutionException())
              }
              //FIXME replace with query string
              def ledgerId = service.ledgerId
              val result = for {
                _ <- Future.successful(applyLedgerValidation(ledgerId, service)).toFutureOr
                x <- queryResult.toFutureOr
              } yield x
              result.future
            }
          } ~
          public { _ =>
            def queryResult = {
              val query = IdentityQuery.RetrieveIdentityKeys(identity)
              val keys = service.executeQuery(service.ledgerId, query)
              fromFutureEither(keys, IdentityQueryExecutionException())
            }
            //FIXME replace with query string
            def ledgerId = service.ledgerId
            val result = for {
              _ <- Future.successful(applyLedgerValidation(ledgerId, service)).toFutureOr
              x <- queryResult.toFutureOr
            } yield x
            result.future
          }
      } ~
        post {
          publicInput(StatusCodes.Created) { ctx: HasModel[CreateIdentityTransactionRequest] =>
            def queryResult = {
              val query = for {
                tx <- service.createIdentityTransaction(ctx.model).onFor

                submitTransaction = toSubmitRequest(tx, ctx.model.ledgerId)
                _ <- service.submitIdentityTransaction(submitTransaction).onFor
              } yield tx

              // The actual method call never fails but the type system says it could, we need this to be able to compile

              fromFutureEither(query.res, IdentityTransactionCreationError())
            }
            //FIXME replace with query string
            def ledgerId = service.ledgerId
            val result = for {
              _ <- Future.successful(applyLedgerValidation(ledgerId, service)).toFutureOr
              x <- queryResult.toFutureOr
            } yield x
            result.future
          }
        }
    }
  }
}

object IdentitiesController {

  final case class IdentityTransactionCreationError(id: ErrorId = ErrorId.create) extends ServerError {
    override def cause: Option[Throwable] = None
  }

  final case class IdentityQueryExecutionException(id: ErrorId = ErrorId.create) extends ServerError {
    override def cause: Option[Throwable] = None
  }

  private def toSubmitRequest(it: IdentityTransaction, ledgerId: LedgerId): SubmitIdentityTransactionRequest = {

    val secondSignatureMaybe = it match {
      case x: Link => Option(x.linkingIdentitySignature)
      case _ => None
    }
    val signatureFromCertificateMaybe = it match {
      case x: LinkCertificate => Option(x.signatureFromCertificate)
      case _ => None
    }
    val (thirdSignatureMaybe, fourthSignatureMaybe) = it match {
      case x: Grant => (Option(x.claimSignature), Option(x.endorseSignature))
      case _ => (None, None)
    }

    SubmitIdentityTransactionRequest(
      `type` = IdentityTransactionType.of(it),
      data = it.data,
      ledgerId = ledgerId,
      signature = it.signature,
      linkingIdentitySignature = secondSignatureMaybe,
      claimSignature = thirdSignatureMaybe,
      endorseSignature = fourthSignatureMaybe,
      signatureFromCertificate = signatureFromCertificateMaybe
    )
  }
}

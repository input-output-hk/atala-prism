package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.models.{ErrorId, ServerError}
import io.iohk.cef.frontend.client.ServiceResponseExtensions
import io.iohk.cef.frontend.controllers.common.Codecs._
import io.iohk.cef.frontend.controllers.common._
import io.iohk.cef.frontend.models.{CreateIdentityTransactionRequest, IdentityTransactionType, SubmitIdentityTransactionRequest, UnsupportedLedgerIdError}
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.ledger.identity.{Grant, IdentityTransaction, Link, LinkCertificate}
import io.iohk.cef.ledger.query.identity.IdentityQuery
import org.scalactic.{Bad, Every}
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class IdentitiesController(service: IdentityTransactionService)(
    implicit mat: Materializer,
    ec: ExecutionContext
) extends CustomJsonController {

  import Context._
  import IdentitiesController._

  private def applyValidations[A](ledgerId: LedgerId, result: => FutureApplicationResult[A]): FutureApplicationResult[A] = {
    if (!service.isLedgerSupported(ledgerId))
      Future.successful(Bad(Every(UnsupportedLedgerIdError(ledgerId))))
    else
      result
  }

  lazy val routes = corsHandler {
    pathPrefix("identities") {
      (get & pathPrefix(Segment)) { identity =>
        path("exists") {
          public { _ =>
            //replace with query string
            def ledgerId = service.ledgerId
            def exec = {
              val query = IdentityQuery.ExistsIdentity(identity)
              val exists = service.executeQuery(service.ledgerId, query)
              val result = exists.map(x => x.map(exists => Json.obj("exists" -> JsBoolean(exists))))
              fromFutureEither(result, IdentityQueryExecutionException())
            }
            applyValidations(ledgerId, exec)
          }
        } ~
          path("endorsers") {
            public { _ =>
              //replace with query string
              def ledgerId = service.ledgerId
              def exec = {
                val query = IdentityQuery.RetrieveEndorsers(identity)
                fromFutureEither(service.executeQuery(service.ledgerId, query), IdentityQueryExecutionException())
              }
              applyValidations(ledgerId, exec)
            }
          } ~
          path("endorsements") {
            public { _ =>
              //replace with query string
              def ledgerId = service.ledgerId
              def exec = {
                val query = IdentityQuery.RetrieveEndorsements(identity)
                val result = service.executeQuery(service.ledgerId, query)
                fromFutureEither(result, IdentityQueryExecutionException())
              }
              applyValidations(ledgerId, exec)
            }
          } ~
          public { _ =>
            //replace with query string
            def ledgerId = service.ledgerId
            def exec = {
              val query = IdentityQuery.RetrieveIdentityKeys(identity)
              val keys = service.executeQuery(service.ledgerId, query)
              fromFutureEither(keys, IdentityQueryExecutionException())
            }
            applyValidations(ledgerId, exec)
          }
      } ~
        post {
          publicInput(StatusCodes.Created) { ctx: HasModel[CreateIdentityTransactionRequest] =>
            //replace with query string
            def ledgerId = service.ledgerId
            def result = {
              val queryResult = for {
                tx <- service.createIdentityTransaction(ctx.model).onFor

                submitTransaction = toSubmitRequest(tx, ctx.model.ledgerId)
                _ <- service.submitIdentityTransaction(submitTransaction).onFor
              } yield tx

              // The actual method call never fails but the type system says it could, we need this to be able to compile

              fromFutureEither(queryResult.res, IdentityTransactionCreationError())
            }
            applyValidations(ledgerId, result)
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

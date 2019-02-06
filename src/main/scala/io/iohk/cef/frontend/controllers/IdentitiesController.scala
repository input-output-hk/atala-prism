package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
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
import io.iohk.cef.query.ledger.identity.{IdentityQuery, IdentityQueryService}
import org.scalactic.Good
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class IdentitiesController(queryService: IdentityQueryService, service: IdentityTransactionService)(
    implicit mat: Materializer,
    ec: ExecutionContext
) extends CustomJsonController {

  import Context._
  import IdentitiesController._

  lazy val routes = corsHandler {
    pathPrefix("identities") {
      (get & pathPrefix(Segment)) { identity =>
        path("exists") {
          public { _ =>
            val query = IdentityQuery.ExistsIdentity(identity)
            val exists = queryService.perform(query)
            val result = Json.obj("exists" -> JsBoolean(exists))
            Future.successful(Good(result))
          }
        } ~
          path("endorsers") {
            public { _ =>
              val query = IdentityQuery.RetrieveEndorsers(identity)
              val result = queryService.perform(query)
              Future.successful(Good(result))
            }
          } ~
          path("endorsements") {
            public { _ =>
              val query = IdentityQuery.RetrieveEndorsements(identity)
              val result = queryService.perform(query)
              Future.successful(Good(result))
            }
          } ~
          public { _ =>
            val query = IdentityQuery.RetrieveIdentityKeys(identity)
            val keys = queryService.perform(query)
            Future.successful(Good(keys))
          }
      } ~
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

package io.iohk.cef.frontend.controllers

import java.util.UUID

import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import io.iohk.cef.agreements.AgreementsService
import io.iohk.cef.utils.NonEmptyList
import io.iohk.cef.frontend.controllers.common.CustomJsonController
import org.scalactic.Good
import play.api.libs.json.{JsObject, Json, Reads}

import scala.concurrent.{ExecutionContext, Future}

class AgreementsGenericController(implicit ec: ExecutionContext, mat: Materializer) extends CustomJsonController {

  import AgreementsGenericController._
  import Context._

  def routes[T](prefix: String, service: AgreementsService[T])(implicit readsT: Reads[T]): Route = {

    pathPrefix("agreements" / prefix) {
      path("agree") {
        post {
          publicInput { ctx: HasModel[AgreeRequest[T]] =>
            Future { service.agree(ctx.model.correlationId, ctx.model.data) }
              .map(_ => Good(JsObject.empty))
          }
        }
      } ~
        path("propose") {
          post {
            publicInput { ctx: HasModel[ProposeRequest[T]] =>
              Future { service.propose(ctx.model.correlationId, ctx.model.data, ctx.model.to.toSet) }
                .map(_ => Good(JsObject.empty))
            }
          }
        } ~
        path("decline") {
          post {
            publicInput { ctx: HasModel[DeclineRequest[T]] =>
              Future { service.decline(ctx.model.correlationId) }
                .map(_ => Good(JsObject.empty))
            }
          }
        }
    }
  }
}

object AgreementsGenericController {

  import io.iohk.cef.agreements.UserId
  import io.iohk.cef.frontend.controllers.common.Codecs.{nodeIdFormat, nonEmptyListFormat}

  case class AgreeRequest[T](correlationId: UUID, data: T)
  case class ProposeRequest[T](correlationId: UUID, data: T, to: NonEmptyList[UserId])
  case class DeclineRequest[T](correlationId: UUID)

  implicit def agreeRequestReads[T](implicit readsT: Reads[T]): Reads[AgreeRequest[T]] = Json.reads[AgreeRequest[T]]

  implicit def proposeRequestReads[T](implicit readsT: Reads[T]): Reads[ProposeRequest[T]] =
    Json.reads[ProposeRequest[T]]

  implicit def declineRequestReads[T](implicit readsT: Reads[T]): Reads[DeclineRequest[T]] =
    Json.reads[DeclineRequest[T]]

}

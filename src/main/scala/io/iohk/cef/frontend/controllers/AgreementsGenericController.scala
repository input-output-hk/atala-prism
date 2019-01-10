package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import io.iohk.cef.agreements.AgreementsService
import io.iohk.cef.data.NonEmptyList
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
            // TODO: Remove Future wrapper when the service returns a scala Future
            Future { service.agree(ctx.model.correlationId, ctx.model.data) }
              .map(_ => Good(JsObject.empty))
          }
        }
      } ~
        path("propose") {
          post {
            publicInput { ctx: HasModel[ProposeRequest[T]] =>
              // TODO: Remove Future wrapper when the service returns a scala Future
              Future { service.propose(ctx.model.correlationId, ctx.model.data, ctx.model.recipients) }
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

  case class AgreeRequest[T](correlationId: String, data: T)
  case class ProposeRequest[T](correlationId: String, data: T, recipients: NonEmptyList[UserId])

  implicit def agreeRequestReads[T](implicit readsT: Reads[T]): Reads[AgreeRequest[T]] = Json.reads[AgreeRequest[T]]

  implicit def proposeRequestReads[T](implicit readsT: Reads[T]): Reads[ProposeRequest[T]] =
    Json.reads[ProposeRequest[T]]
}

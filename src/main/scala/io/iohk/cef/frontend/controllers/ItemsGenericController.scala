package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import io.iohk.cef.frontend.controllers.common.CustomJsonController
import org.scalactic.Good
import play.api.libs.json.{Format, JsObject, JsString}

import scala.concurrent.{ExecutionContext, Future}

class ItemsGenericController(implicit ec: ExecutionContext, mat: Materializer) extends CustomJsonController {

  import Context._

  def routes[T](prefix: String)(implicit format: Format[T]): Route = {
    pathPrefix(prefix) {
      pathEnd {
        post {
          publicInput(StatusCodes.Created) { ctx: HasModel[T] =>
            // TODO: complete it
            val r = JsObject.empty + ("status" -> JsString("OK"))
            Future.successful(Good(r))
          }
        }
      }
    }
  }
}

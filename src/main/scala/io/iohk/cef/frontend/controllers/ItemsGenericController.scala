package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{GenericPublicError, InputValidationError, PublicError}
import io.iohk.cef.data.{DataItem, DataItemService}
import io.iohk.cef.frontend.controllers.common.{CustomJsonController, _}
import io.iohk.cef.ledger.ByteStringSerializable
import play.api.libs.json.{JsObject, Reads}

import scala.concurrent.{ExecutionContext, Future}

class ItemsGenericController(service: DataItemService)(implicit ec: ExecutionContext, mat: Materializer)
    extends CustomJsonController {

  import Context._
  import ItemsGenericController._

  def routes[B, A <: DataItem[B]](
      prefix: String)(implicit format: Reads[A], itemSerializable: ByteStringSerializable[B]): Route = {
    pathPrefix(prefix) {
      pathEnd {
        post {
          publicInput(StatusCodes.Created) { ctx: HasModel[A] =>
            val either = service.insert(ctx.model)
            val result = fromEither(either, ItemCreationError)
              .map(_ => JsObject.empty)

            Future.successful(result)
          }
        }
      }
    }
  }
}

object ItemsGenericController {

  case object ItemCreationError extends InputValidationError {
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("Failed to create item")
      val error = GenericPublicError(message)
      List(error)
    }
  }
}

package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{GenericPublicError, InputValidationError, PublicError}
import io.iohk.cef.data.{CanValidate, DataItem, DataItemService}
import io.iohk.cef.frontend.controllers.common.{CustomJsonController, _}
import io.iohk.cef.frontend.models.DataItemEnvelope
import play.api.libs.json.{JsObject, Reads}
import io.iohk.cef.codecs.nio._

import scala.concurrent.{ExecutionContext, Future}

class ItemsGenericController(service: DataItemService)(implicit ec: ExecutionContext, mat: Materializer)
    extends CustomJsonController {

  import Context._
  import ItemsGenericController._

  def routes[D, E <: DataItemEnvelope[DataItem[D]]](prefix: String)(
      implicit format: Reads[E],
      itemSerializable: NioEncDec[D],
      canValidate: CanValidate[DataItem[D]]): Route = {
    pathPrefix(prefix) {
      pathEnd {
        post {
          publicInput(StatusCodes.Created) { ctx: HasModel[E] =>
            val either = service.insert(ctx.model.tableId, ctx.model.content)
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

package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{GenericPublicError, InputValidationError, PublicError}
import io.iohk.cef.data.{CanValidate, DataItem, DataItemAction, DataItemService}
import io.iohk.cef.frontend.controllers.common.{CustomJsonController, _}
import play.api.libs.json.{JsObject, Reads}
import io.iohk.cef.codecs.nio._
import io.iohk.cef.core.Envelope

import scala.concurrent.{ExecutionContext, Future}

class ItemsGenericController(implicit ec: ExecutionContext, mat: Materializer) extends CustomJsonController {

  import Context._
  import ItemsGenericController._

  def routes[D](prefix: String, service: DataItemService[D])(
      implicit format: Reads[Envelope[DataItem[D]]],
      itemSerializable: NioEncDec[D],
      canValidate: CanValidate[DataItem[D]]): Route = {
    pathPrefix(prefix) {
      pathEnd {
        post {
          publicInput(StatusCodes.Created) { ctx: HasModel[Envelope[DataItem[D]]] =>
            val either = service.processAction(ctx.model.map(DataItemAction.Insert.apply))
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

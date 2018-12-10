package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{GenericPublicError, InputValidationError, PublicError}
import io.iohk.cef.data._
import io.iohk.cef.frontend.controllers.common.{CustomJsonController, _}
import io.iohk.cef.frontend.models.DataItemIdentifier
import play.api.libs.json.Reads
import io.iohk.cef.codecs.nio._
import io.iohk.cef.transactionservice.Envelope
import io.iohk.cef.frontend.controllers.common.Codecs.DataItemServiceResponseWrites

import scala.concurrent.{ExecutionContext, Future}

class ItemsGenericController(implicit ec: ExecutionContext, mat: Materializer) extends CustomJsonController {

  import Context._
  import ItemsGenericController._

  def routes[D](prefix: String, service: DataItemService[D])(
      implicit format: Reads[Envelope[DataItem[D]]],
      identifierFormat: Reads[Envelope[DataItemIdentifier]],
      itemSerializable: NioCodec[D],
      canValidate: CanValidate[DataItem[D]]): Route = {
    pathPrefix(prefix) {
      path("validation") {
        post {
          publicInput(StatusCodes.OK) { ctx: HasModel[Envelope[DataItem[D]]] =>
            val validateAction: Envelope[DataItemAction[D]] =
              ctx.model.copy(content = DataItemAction.ValidateAction(ctx.model.content))
            val either = service.processAction(validateAction)
            val result = fromEither(either, ItemValidationError)

            Future.successful(result)
          }
        }
      } ~
        path("delete") {
          post {
            publicInput(StatusCodes.OK) { ctx: HasModel[Envelope[DataItemIdentifier]] =>
              val insertAction: Envelope[DataItemAction[D]] =
                ctx.model.copy(content = DataItemAction.DeleteAction(ctx.model.content.id, ctx.model.content.signature))
              val either = service.processAction(insertAction)
              val result = fromEither(either, ItemDeleteError)

              Future.successful(result)
            }
          }
        } ~
        pathEnd {
          post {
            publicInput(StatusCodes.Created) { ctx: HasModel[Envelope[DataItem[D]]] =>
              val insertAction: Envelope[DataItemAction[D]] =
                ctx.model.copy(content = DataItemAction.InsertAction(ctx.model.content))
              val either = service.processAction(insertAction)
              val result = fromEither(either, ItemCreationError)

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

  case object ItemValidationError extends InputValidationError {
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("Failed to validate an item")
      val error = GenericPublicError(message)
      List(error)
    }
  }

  case object ItemDeleteError extends InputValidationError {
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("Failed to delete an item")
      val error = GenericPublicError(message)
      List(error)
    }
  }
}

package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{GenericPublicError, InputValidationError, PublicError, _}
import io.iohk.codecs.nio._
import io.iohk.cef.data.query.DataItemQuery
import io.iohk.cef.data.{CanValidate, DataItem, DataItemAction, DataItemService}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.controllers.common.Codecs.DataItemServiceResponseWrites
import io.iohk.cef.frontend.controllers.common.{CustomJsonController, _}
import io.iohk.cef.frontend.models.DataItemIdentifier
import io.iohk.network.Envelope
import play.api.libs.json.{Reads, Writes}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class ItemsGenericController(implicit ec: ExecutionContext, mat: Materializer) extends CustomJsonController {

  import Context._
  import ItemsGenericController._

  def routes[D](prefix: String, service: DataItemService[D], queryResultTimeout: FiniteDuration)(
      implicit format: Reads[Envelope[DataItem[D]]],
      queryFormat: Reads[Envelope[DataItemQuery]],
      queryResponseFormat: Writes[Seq[DataItem[D]]],
      identifierFormat: Reads[Envelope[DataItemIdentifier]],
      itemSerializable: NioCodec[D],
      canValidate: CanValidate[DataItem[D]]
  ): Route = corsHandler {
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
          } ~
            get {
              publicInput(StatusCodes.OK) { ctx: HasModel[Envelope[DataItemQuery]] =>
                val futureEither = service
                  .processQuery(ctx.model)
                  .withTimeout(queryResultTimeout)
                  .fold[Either[ApplicationError, Seq[DataItem[D]]]](Right(Seq()))(
                    (state, current) =>
                      for {
                        s <- state
                        c <- current
                      } yield c ++ s
                  )

                futureEither.map { either =>
                  fromEither(either, QueryEngineError)
                }
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

  case object QueryEngineError extends ServerError {
    //TODO Define error Ids and handling
    override def id: ErrorId = ErrorId.create

    override def cause: Option[Throwable] = None
  }
}

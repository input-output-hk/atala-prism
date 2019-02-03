package io.iohk.cef.frontend.controllers.common

import akka.http.scaladsl.model.HttpRequest
import com.alexitc.playsonify.akka._
import com.alexitc.playsonify.models.{ErrorId, ServerError}
import org.slf4j.LoggerFactory

abstract class CustomJsonController
    extends AbstractJsonController(new CustomJsonController.CustomJsonComponents)
    with CORSHandler {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  override protected def onServerError(error: ServerError, id: ErrorId): Unit = {
    error.cause
      .orElse {
        logger.error(s"Server error: $error, id = ${error.id}")
        None
      }
      .foreach { _ =>
        logger.error(s"Server error: $error, id = $id")
      }
  }
}

object CustomJsonController {

  class CustomJsonComponents extends JsonControllerComponents[Nothing] {

    override def i18nService: SingleLangService = SingleLangService.Default

    override def publicErrorRenderer: PublicErrorRenderer = new PublicErrorRenderer

    override def authenticatorService: AbstractAuthenticatorService[Nothing] =
      (_: HttpRequest) => {
        throw new RuntimeException("Authentication is not supported.")
      }
  }
}

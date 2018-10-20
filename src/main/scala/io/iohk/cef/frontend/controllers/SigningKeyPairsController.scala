package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import io.iohk.cef.frontend.controllers.common.{Codecs, CustomJsonController}
import io.iohk.cef.frontend.services.CryptoService
import org.scalactic.Good

import scala.concurrent.{ExecutionContext, Future}

class SigningKeyPairsController(service: CryptoService)(implicit ec: ExecutionContext, mat: Materializer)
    extends CustomJsonController {

  import Codecs._

  lazy val routes: Route = {
    path("signing-key-pairs") {
      post {
        public(StatusCodes.Created) { _ =>
          val pair = service.getSigningKeyPair()
          Future.successful(Good(pair))
        }
      }
    }
  }
}

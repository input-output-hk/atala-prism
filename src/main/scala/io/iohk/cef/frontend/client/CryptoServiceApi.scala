package io.iohk.cef.frontend.client

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import io.iohk.cef.frontend.services.CryptoService

import scala.concurrent.ExecutionContext

class CryptoApi(service: CryptoService)(implicit ec: ExecutionContext) extends Directives {

  def generateSigningKeyPair: Route = {
    path("signing-key-pair") {
      get {
        val ret = service.getSigningKeyPair
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, ret.toString))
      }
    }
  }
}

package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.services.CryptoService
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.JsValue

class SigningKeyPairsControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  import Codecs._

  implicit val executionContext = system.dispatcher

  val service = new CryptoService
  val controller = new SigningKeyPairsController(service)
  lazy val routes = controller.routes

  "POST /signing-key-pairs" should {
    "create a valid key-pair" in {
      val request = Post("/signing-key-pairs", HttpEntity(ContentTypes.`application/json`, "{}"))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)

        val json = responseAs[JsValue]

        val publicKey = (json \ "publicKey").as[String]
        val privateKey = (json \ "privateKey").as[String]

        SigningPublicKey.decodeFrom(fromHex(publicKey)).isRight must be(true)
        SigningPrivateKey.decodeFrom(fromHex(privateKey)).isRight must be(true)
      }
    }
  }
}

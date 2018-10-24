package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.alexitc.playsonify.akka.PublicErrorRenderer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.core.NodeCore
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.JsValue

import scala.concurrent.Future

class IdentitiesControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  import Codecs._

  val nodeCore = mock[NodeCore[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction]]

  when(nodeCore.receiveTransaction(any())).thenReturn(Future.successful(Right(())))
  implicit val executionContext = system.dispatcher

  val service = new IdentityTransactionService(nodeCore)
  val controller = new IdentitiesController(service)
  lazy val routes = controller.routes

  "POST /identities" should {
    val pair = generateSigningKeyPair()
    val publicKeyHex = toCleanHex(pair.public.toByteString)
    val privateKeyHex = toCleanHex(pair.`private`.toByteString)

    def testTransactionType(txType: String) = {
      val identity = "iohk"

      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "identity": "$identity",
           |    "ledgerId": 1,
           |    "publicKey": "$publicKeyHex",
           |    "privateKey": "$privateKeyHex"
           |}
         """.stripMargin

      val request = Post("/identities", jsonEntity(body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)

        val json = responseAs[JsValue]
        (json \ "type").as[String] must be(txType)
        (json \ "key").as[String] must be(publicKeyHex)
        (json \ "signature").as[String] mustNot be(empty)
        (json \ "identity").as[String] must be(identity)
      }
    }

    def validateErrorResponse(json: JsValue) = {
      val errors = (json \ "errors").as[List[JsValue]]
      errors.size must be(5)
      errors.foreach { error =>
        (error \ "type").as[String] must be(PublicErrorRenderer.FieldValidationErrorType)
        (error \ "message").as[String] mustNot be(empty)
        (error \ "field").as[String] mustNot be(empty)
      }
    }

    "be able to create identity claim transaction" in {
      testTransactionType("Claim")
    }

    "be able to create identity link transaction" in {
      testTransactionType("Link")
    }

    "be able to create identity unlink transaction" in {
      testTransactionType("Unlink")
    }

    "return validation errors" in {
      val body =
        s"""
           |{
           |    "type": "none",
           |    "identity": 2,
           |    "ledgerId": "none",
           |    "publicKey": "1$publicKeyHex",
           |    "privateKey": "2$privateKeyHex"
           |}
         """.stripMargin

      val request = Post("/identities", jsonEntity(body))

      request ~> routes ~> check {
        status must ===(StatusCodes.BadRequest)

        val json = responseAs[JsValue]
        validateErrorResponse(json)
      }
    }

    "return missing field errors" in {
      val request = Post("/identities", jsonEntity("{}"))

      request ~> routes ~> check {
        status must ===(StatusCodes.BadRequest)

        val json = responseAs[JsValue]
        validateErrorResponse(json)
      }
    }
  }

  private def jsonEntity(body: String) = HttpEntity(ContentTypes.`application/json`, body)
}

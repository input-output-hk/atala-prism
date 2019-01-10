package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import com.alexitc.playsonify.akka.PublicErrorRenderer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.crypto._
import io.iohk.cef.crypto.certificates.test.data.ExampleCertificates.twoChainedCertsPEM
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity._
import io.iohk.cef.query.ledger.identity.{IdentityQueryEngine, IdentityQueryService}
import io.iohk.cef.transactionservice.NodeTransactionService
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{Assertion, MustMatchers, WordSpec}
import play.api.libs.json.JsValue

import scala.concurrent.Future

class IdentitiesControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  import Codecs._

  val nodeTransactionService = mock[NodeTransactionService[IdentityData, IdentityTransaction]]

  when(nodeTransactionService.receiveTransaction(any())).thenReturn(Future.successful(Right(())))
  implicit val executionContext = system.dispatcher

  val queryEngine = mock[IdentityQueryEngine]
  val queryService = new IdentityQueryService(queryEngine)
  val service = new IdentityTransactionService(nodeTransactionService)
  val controller = new IdentitiesController(queryService, service)
  lazy val routes = controller.routes

  "GET /identities/:identity" should {
    "return the identity keys" in {
      val identity = "iohk"
      val key = generateSigningKeyPair().public
      when(queryEngine.get(anyString())).thenReturn(Option(IdentityData.forKeys(key)))

      val request = Get(s"/identities/$identity")

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        val list = json.as[List[JsValue]]
        list.size must be(1)
        list.head.as[String] must be(key.toString())
      }
    }
  }

  "GET /identities/:identity/exists" should {
    "return whether the identity exists" in {
      val identity = "iohk"
      when(queryEngine.contains(identity)).thenReturn(true)

      val request = Get(s"/identities/$identity/exists")

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "exists").as[Boolean] must be(true)
      }
    }
  }

  "POST /identities" should {
    val pair = generateSigningKeyPair()
    val publicKeyHex = toCleanHex(pair.public.toByteString)
    val privateKeyHex = toCleanHex(pair.`private`.toByteString)

    def testTransactionType(txType: String, dataType: String): Assertion = {
      val identity = "iohk"

      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "identity": "$identity",
           |    "data": {
           |      "_type":"$dataType",
           |      "identity": "$identity",
           |      "key": "$publicKeyHex"
           |      },
           |    "ledgerId": "1",
           |    "publicKey": "$publicKeyHex",
           |    "privateKey": "$privateKeyHex"
           |}
         """.stripMargin

      val request = Post("/identities", jsonEntity(body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)

        val json = responseAs[JsValue]
        (json \ "type").as[String] must be(txType)
        (json \ "data" \ "identity").as[String] must be(identity)
        (json \ "data" \ "key").as[String] must be(pair.public.toString)
        (json \ "signature").as[String] mustNot be(empty)
      }
    }

    def testEndorseType(
        endorserIdentity: String,
        endorsedIdentity: String,
        privateKey: SigningPrivateKey
    ): Assertion = {
      val txType = "Endorse"
      val privateKeyHex = toCleanHex(privateKey.toByteString)

      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "data": {
           |      "_type":"io.iohk.cef.ledger.identity.EndorseData",
           |      "endorserIdentity": "$endorserIdentity",
           |      "endorsedIdentity": "$endorsedIdentity"
           |    },
           |    "ledgerId": "1",
           |    "privateKey": "$privateKeyHex"
           |
           |}
         """.stripMargin

      val request = Post("/identities", jsonEntity(body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
        val json = responseAs[JsValue]
        (json \ "type").as[String] must be(txType)
        (json \ "data" \ "endorserIdentity").as[String] must be(endorserIdentity)
        (json \ "data" \ "endorsedIdentity").as[String] must be(endorsedIdentity)
        (json \ "signature").as[String] mustNot be(empty)
      }
    }

    def testTransactionGrantType(txType: String): Assertion = {
      val pairLink = generateSigningKeyPair()
      val privateKeyLinkHex = toCleanHex(pairLink.`private`.toByteString)
      val grantedIdentity = "granted"
      val grantingIdentity = "granting"

      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "data": {
           |      "_type":"io.iohk.cef.ledger.identity.GrantData",
           |      "grantingIdentity": "$grantingIdentity",
           |      "grantedIdentity" : "$grantedIdentity",
           |      "grantedIdentityPublicKey": "${publicKeyHex}"
           |      },
           |    "ledgerId": "1",
           |    "privateKey": "$privateKeyHex",
           |    "linkingIdentityPrivateKey": "$privateKeyLinkHex"
           |}
         """.stripMargin

      val request = Post("/identities", jsonEntity(body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)

        val json = responseAs[JsValue]
        (json \ "type").as[String] must be(txType)
        (json \ "data" \ "grantingIdentity").as[String] must be(grantingIdentity)
        (json \ "data" \ "grantedIdentity").as[String] must be(grantedIdentity)
        (json \ "data" \ "grantedIdentityPublicKey").as[String] must be(pair.public.toString)
      }
    }

    def testTransactionLinkType(txType: String, txDataType: String): Assertion = {

      val pairLink = generateSigningKeyPair()
      val privateKeyLinkHex = toCleanHex(pairLink.`private`.toByteString)

      val identity = "iohk"

      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "data": {
           |      "_type":"$txDataType",
           |      "identity": "$identity",
           |      "key": "$publicKeyHex"
           |    },
           |    "ledgerId": "1",
           |    "privateKey": "$privateKeyHex",
           |    "linkingIdentityPrivateKey": "$privateKeyLinkHex"
           |
           |}
         """.stripMargin

      val request = Post("/identities", jsonEntity(body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
        val json = responseAs[JsValue]
        (json \ "type").as[String] must be(txType)
        (json \ "data" \ "key").as[String] must be(pair.public.toString)
        (json \ "data" \ "identity").as[String] must be(identity)
        (json \ "signature").as[String] mustNot be(empty)
        (json \ "linkingIdentitySignature").as[String] mustNot be(empty)

      }
    }

    def testRevokeType(endorserIdentity: String, endorsedIdentity: String, privateKey: SigningPrivateKey): Assertion = {
      val txType = "Revoke"
      val privateKeyHex = toCleanHex(privateKey.toByteString)

      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "data": {
           |      "_type":"io.iohk.cef.ledger.identity.RevokeEndorsementData",
           |      "endorserIdentity": "$endorserIdentity",
           |      "endorsedIdentity": "$endorsedIdentity"
           |    },
           |    "ledgerId": "1",
           |    "privateKey": "$privateKeyHex"
           |
           |}
         """.stripMargin

      val request = Post("/identities", jsonEntity(body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
        val json = responseAs[JsValue]
        (json \ "type").as[String] must be(txType)
        (json \ "data" \ "endorserIdentity").as[String] must be(endorserIdentity)
        (json \ "data" \ "endorsedIdentity").as[String] must be(endorsedIdentity)
        (json \ "signature").as[String] mustNot be(empty)
      }
    }

    def testTransactionLinkCertificate(txType: String, txDataType: String): Assertion = {
      val pem = twoChainedCertsPEM
      val pemHex = toCleanHex(ByteString(twoChainedCertsPEM))

      val pairLink = generateSigningKeyPair()
      val privateKeyLinkHex = toCleanHex(pairLink.`private`.toByteString)

      val identity = "iohk"

      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "data": {
           |      "_type":"$txDataType",
           |      "linkingIdentity": "$identity",
           |      "pem": "$pemHex"
           |    },
           |    "ledgerId": "1",
           |    "privateKey": "$privateKeyHex",
           |    "linkingIdentityPrivateKey": "$privateKeyLinkHex"
           |
           |}
         """.stripMargin

      val request = Post("/identities", jsonEntity(body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
        val json = responseAs[JsValue]
        (json \ "type").as[String] must be(txType)
        (json \ "data" \ "pem").as[String] must be(pem)
        (json \ "data" \ "linkingIdentity").as[String] must be(identity)
        (json \ "signature").as[String] mustNot be(empty)
        (json \ "signatureFromCertificate").as[String] mustNot be(empty)

      }
    }

    def validateErrorResponse(json: JsValue): Unit = {
      val errors = (json \ "errors").as[List[JsValue]]
      errors.size must be(4)
      errors.foreach { error =>
        (error \ "type").as[String] must be(PublicErrorRenderer.FieldValidationErrorType)
        (error \ "message").as[String] mustNot be(empty)
        (error \ "field").as[String] mustNot be(empty)
      }
    }

    "be able to create identity claim transaction" in {
      testTransactionType("Claim", "io.iohk.cef.ledger.identity.ClaimData")
    }

    "be able to create identity link transaction" in {
      testTransactionLinkType("Link", "io.iohk.cef.ledger.identity.LinkData")
    }

    "be able to create identity unlink transaction" in {
      testTransactionType("Unlink", "io.iohk.cef.ledger.identity.UnlinkData")
    }

    "be able to create a grant transaction" in {
      testTransactionGrantType("Grant")
    }

    "be able to create an endorse transaction" in {
      testEndorseType("endorsing", "endorsed", pair.`private`)
    }

    "be able to create an revoke transaction" in {
      testRevokeType("endorsing", "endorsed", pair.`private`)
    }

    "be able to create an LinkCertificate transaction" in {
      testTransactionLinkCertificate("LinkCertificate", "io.iohk.cef.ledger.identity.LinkCertificateData")
    }

    "return validation errors" in {
      val body =
        s"""
           |{
           |    "type": "none",
           |    "identity": 2,
           |    "ledgerId": 1,
           |    "data": "1$publicKeyHex",
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

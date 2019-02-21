package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import com.alexitc.playsonify.akka.PublicErrorRenderer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.query.identity.{IdentityQuery, IdentityQueryEngine, IdentityQueryService}
import io.iohk.cef.transactionservice.NodeTransactionService
import io.iohk.crypto._
import io.iohk.crypto.certificates.test.data.ExampleCertificates._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{Assertion, MustMatchers, WordSpec}
import play.api.libs.json.JsValue
import org.scalatest.OptionValues._

import scala.concurrent.Future

class IdentitiesControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  import Codecs._

  val nodeTransactionService = mock[NodeTransactionService[IdentityData, IdentityTransaction, IdentityQuery]]
  val ledgerId = "1"

  when(nodeTransactionService.receiveTransaction(any())).thenReturn(Future.successful(Right(())))
  when(nodeTransactionService.supportedLedgerIds).thenReturn(Set(ledgerId))
  val queryEngine = mock[IdentityQueryEngine]
  val queryService = new IdentityQueryService(queryEngine)
  when(nodeTransactionService.getQueryService(ledgerId)).thenReturn(queryService)
  implicit val executionContext = system.dispatcher

  val service = new IdentityTransactionService(nodeTransactionService)
  val controller = new IdentitiesController(service)
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

  "GET /identities/:identity/endorsers" should {
    "return the identities that endorsed another identity" in {
      val identity = "iohk"
      val endorsers = Set("a", "b")
      val data = IdentityData.empty.copy(endorsers = endorsers)
      when(queryEngine.get(anyString())).thenReturn(Option(data))

      val request = Get(s"/identities/$identity/endorsers")

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        val result = json.as[Set[String]]
        result must be(endorsers)
      }
    }
  }

  "GET /identities/:identity/endorsements" should {
    "return the identities that an identity has endorsed" in {
      val identity = "iohk"
      val endorsements = Set("a", "b")

      when(queryEngine.keys()).thenReturn(Set(identity, "a", "b", "c"))
      when(queryEngine.get(identity)).thenReturn(Option(IdentityData.empty))
      when(queryEngine.get("a")).thenReturn(Option(IdentityData.empty.copy(endorsers = Set("b", identity))))
      when(queryEngine.get("b")).thenReturn(Option(IdentityData.empty.copy(endorsers = Set("a", identity))))
      when(queryEngine.get("c")).thenReturn(Option(IdentityData.empty.copy(endorsers = Set("b", "a"))))

      val request = Get(s"/identities/$identity/endorsements")

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        val result = json.as[Set[String]]
        result must be(endorsements)
      }
    }
  }

  "POST /identities" should {
    val pair = generateSigningKeyPair()
    val publicKeyHex = toCleanHex(pair.public.toByteString)
    val privateKeyHex = toCleanHex(pair.`private`.toByteString)

    def testTransactionType(
        txType: String,
        privateKey: SigningPrivateKey,
        identity: Option[String] = None,
        key: Option[SigningPublicKey] = None,
        endorserIdentity: Option[String] = None,
        endorsedIdentity: Option[String] = None,
        grantingIdentity: Option[String] = None,
        grantedIdentity: Option[String] = None,
        grantedIdentityPublicKey: Option[SigningPublicKey] = None,
        linkingIdentityPrivateKey: Option[SigningPrivateKey] = None
    ): Assertion = {

      val identityString = identity.getOrElse("")
      val keyString = key.map(_.toByteString).map(toCleanHex).getOrElse("")

      val endorserIdentityString = endorserIdentity.getOrElse("")
      val endorsedIdentityString = endorsedIdentity.getOrElse("")

      val grantingIdentityString = grantingIdentity.getOrElse("")
      val grantedIdentityString = grantedIdentity.getOrElse("")
      val grantedIdentityPublicKeyString = grantedIdentityPublicKey.map(_.toByteString).map(toCleanHex).getOrElse("")

      val privateKeyString = toCleanHex(privateKey.toByteString)
      val partialBody =
        s"""
           |    "type": "$txType",
           |    "data": {
           |      "identity": "$identityString",
           |      "key": "$keyString",
           |
           |      "endorserIdentity": "$endorserIdentityString",
           |      "endorsedIdentity": "$endorsedIdentityString",
           |
           |      "grantingIdentity": "$grantingIdentityString",
           |      "grantedIdentity" : "$grantedIdentityString",
           |      "grantedIdentityPublicKey": "$grantedIdentityPublicKeyString"
           |    },
           |    "ledgerId": "${ledgerId}",
           |    "privateKey": "$privateKeyString"
         """.stripMargin

      val body = linkingIdentityPrivateKey
        .map { key =>
          val linkingIdentityPrivateKeyString = toCleanHex(key.toByteString)

          s"""
               |{
               |$partialBody,
               |  "linkingIdentityPrivateKey": "$linkingIdentityPrivateKeyString"
               |}
             """.stripMargin
        }
        .getOrElse(s"{$partialBody}")

      val request = Post("/identities", jsonEntity(body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)

        val json = responseAs[JsValue]

        identity.foreach { (json \ "data" \ "identity").as[String] must be(_) }
        key.foreach { k =>
          (json \ "data" \ "key").as[String] must be(k.toString)
        }

        endorserIdentity.foreach { (json \ "data" \ "endorserIdentity").as[String] must be(_) }
        endorsedIdentity.foreach { (json \ "data" \ "endorsedIdentity").as[String] must be(_) }

        grantingIdentity.foreach { (json \ "data" \ "grantingIdentity").as[String] must be(_) }
        grantedIdentity.foreach { (json \ "data" \ "grantedIdentity").as[String] must be(_) }
        grantedIdentityPublicKey.foreach { k =>
          (json \ "data" \ "grantedIdentityPublicKey").as[String] must be(k.toString)
        }

        (json \ "type").as[String].toLowerCase must be(txType.toLowerCase)
        (json \ "signature").as[String] mustNot be(empty)
      }
    }

    def testTransactionLinkType(txType: String): Assertion = {
      val pairLink = generateSigningKeyPair()
      val privateKeyLinkHex = toCleanHex(pairLink.`private`.toByteString)
      val publicKeyLinkHex = toCleanHex(pairLink.public.toByteString)

      val identity = "iohk"
      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "data": {
           |      "identity": "$identity",
           |      "key": "$publicKeyLinkHex"
           |    },
           |    "ledgerId": "${ledgerId}",
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
        (json \ "data" \ "key").as[String] must be(pairLink.public.toString)
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
           |      "endorserIdentity": "$endorserIdentity",
           |      "endorsedIdentity": "$endorsedIdentity"
           |    },
           |    "ledgerId": "${ledgerId}",
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

    def testTransactionLinkCertificate(txType: String): Assertion = {
      val pem = twoChainedCertsPEM
      val pemHex = toCleanHex(ByteString(twoChainedCertsPEM))
      val privateKeyLinkHex = toCleanHex(toSigningPrivateKey(validCertPrivateKey).value.toByteString)

      val identity = "valid"

      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "data": {
           |      "linkingIdentity": "$identity",
           |      "pem": "$pemHex"
           |    },
           |    "ledgerId": "${ledgerId}",
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

    "be able to create identity claim transaction" in {
      testTransactionType("Claim", pair.`private`, identity = Some("iohk"), key = Some(pair.public))
    }

    "be able to create identity link transaction" in {
      testTransactionLinkType("Link")
    }

    "be able to create identity unlink transaction" in {
      testTransactionType("Unlink", pair.`private`, identity = Some("iohk"), key = Some(pair.public))
    }

    "be able to create a grant transaction" in {
      val keys = generateSigningKeyPair()
      testTransactionType(
        "Grant",
        pair.`private`,
        grantedIdentity = Some("granted"),
        grantingIdentity = Some("granting"),
        grantedIdentityPublicKey = Some(keys.public),
        linkingIdentityPrivateKey = Some(keys.`private`)
      )
    }

    "be able to create an endorse transaction" in {
      testTransactionType(
        "endorse",
        pair.`private`,
        endorserIdentity = Some("endorsing"),
        endorsedIdentity = Some("endorsed")
      )
    }

    "be able to create an revoke transaction" in {
      testRevokeType("endorsing", "endorsed", pair.`private`)
    }

    "be able to create an LinkCertificate transaction" in {
      testTransactionLinkCertificate("LinkCertificate")
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
        validateErrorResponse(json, 1)
      }
    }

    "return data errors" in {
      val body =
        s"""
           |{
           |    "type": "claim",
           |    "identity": "x",
           |    "ledgerId": "${ledgerId}",
           |    "data": {},
           |    "privateKey": "$privateKeyHex"
           |}
         """.stripMargin

      val request = Post("/identities", jsonEntity(body))

      request ~> routes ~> check {
        status must ===(StatusCodes.BadRequest)

        val json = responseAs[JsValue]
        validateErrorResponse(json, 2)
      }
    }

    "return missing field errors" in {
      val request = Post("/identities", jsonEntity("{}"))

      request ~> routes ~> check {
        status must ===(StatusCodes.BadRequest)

        val json = responseAs[JsValue]
        validateErrorResponse(json, 1)
      }
    }
  }

  private def validateErrorResponse(json: JsValue, size: Int): Unit = {
    val errors = (json \ "errors").as[List[JsValue]]
    errors.size must be(size)
    errors.foreach { error =>
      (error \ "type").as[String] must be(PublicErrorRenderer.FieldValidationErrorType)
      (error \ "message").as[String] mustNot be(empty)
      (error \ "field").as[String] mustNot be(empty)
    }
  }

  private def jsonEntity(body: String) = HttpEntity(ContentTypes.`application/json`, body)
}

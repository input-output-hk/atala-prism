package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.util.ByteString
import com.alexitc.playsonify.akka.PublicErrorRenderer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.query.identity.{IdentityPartition, IdentityQuery, IdentityQueryEngine, IdentityQueryService}
import io.iohk.cef.ledger.query.{LedgerQuery, LedgerQueryService}
import io.iohk.cef.ledger.{Block, LedgerId, Transaction, UnsupportedLedgerException}
import io.iohk.cef.transactionservice.NodeTransactionService
import io.iohk.crypto._
import io.iohk.crypto.certificates.test.data.ExampleCertificates._
import io.iohk.network.Envelope
import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.{Assertion, WordSpec}
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class IdentitiesControllerSpec extends WordSpec with ScalatestRouteTest {

  import Codecs._
  import IdentitiesControllerSpec._

  implicit val executionContext = system.dispatcher
  implicit val timeout = RouteTestTimeout(5.seconds)

  val ledgerId = "1"

  def transactionService(queryEngine: IdentityQueryEngine) =
    new DummyNodeTransactionService[IdentityData, IdentityTransaction, IdentityQuery] {
      override def receiveTransaction(
          txEnvelope: Envelope[IdentityTransaction]
      ): Future[Either[ApplicationError, Unit]] = {
        Future.successful(Right(()))
      }

      override def supportedLedgerIds: Set[LedgerId] = {
        Set(ledgerId)
      }

      override def getQueryService(ledgerId: LedgerId): LedgerQueryService[IdentityData, IdentityQuery] = {
        if (supportedLedgerIds contains ledgerId)
          new IdentityQueryService(queryEngine)
        else
          throw UnsupportedLedgerException(ledgerId)
      }
    }

  val defaultQueryEngine = new InMemoryIdentityQueryEngine(Map.empty)
  val defaultTransactionService = transactionService(defaultQueryEngine)

  def routes(
      transactionService: NodeTransactionService[IdentityData, IdentityTransaction, IdentityQuery] =
        defaultTransactionService
  ) = {

    val service = new IdentityTransactionService(transactionService)
    val controller = new IdentitiesController(service)
    controller.routes
  }

  def routes(queryEngine: IdentityQueryEngine) = {
    val service = new IdentityTransactionService(transactionService(queryEngine))
    val controller = new IdentitiesController(service)
    controller.routes
  }

  private def withLedgerId(path: String) = s"/ledgers/$ledgerId$path"

  "GET /identities" should {
    "return the existing identities" in {
      val identities = Set("iohk", "IOHK", "ioHK")
      val map = identities.map(_ -> IdentityData.empty).toMap
      val queryEngine = new InMemoryIdentityQueryEngine(map)

      val request = Get(withLedgerId(s"/identities"))

      request ~> routes(queryEngine) ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        val result = json.as[Set[String]]
        result must be(identities)
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId("/identities")
    }
  }

  "GET /identities/:identity" should {
    val identity = "iohk"

    "return the identity keys" in {
      val key = generateSigningKeyPair().public
      val map = Map(identity -> IdentityData.forKeys(key))
      val queryEngine = new InMemoryIdentityQueryEngine(map)

      val request = Get(withLedgerId(s"/identities/$identity"))

      request ~> routes(queryEngine) ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        val list = json.as[List[JsValue]]
        list.size must be(1)
        list.head.as[String] must be(key.toString())
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId(s"/identities/$identity")
    }
  }

  "GET /identities/:identity/exists" should {
    val identity = "iohk"

    "return whether the identity exists" in {
      val map = Map(identity -> IdentityData.empty)
      val queryEngine = new InMemoryIdentityQueryEngine(map)

      val request = Get(withLedgerId(s"/identities/$identity/exists"))

      request ~> routes(queryEngine) ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "exists").as[Boolean] must be(true)
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId(s"/identities/$identity/exists")
    }
  }

  "GET /identities/:identity/endorsers" should {
    val identity = "iohk"

    "return the identities that endorsed another identity" in {
      val endorsers = Set("a", "b")
      val data = IdentityData.empty.copy(endorsers = endorsers)
      val map = Map(identity -> data)
      val queryEngine = new InMemoryIdentityQueryEngine(map)

      val request = Get(withLedgerId(s"/identities/$identity/endorsers"))

      request ~> routes(queryEngine) ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        val result = json.as[Set[String]]
        result must be(endorsers)
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId(s"/identities/$identity/endorsers")
    }
  }

  "GET /identities/:identity/endorsements" should {
    val identity = "iohk"

    "return the identities that an identity has endorsed" in {
      val endorsements = Set("a", "b")

      val map = Map(
        identity -> IdentityData.empty,
        "a" -> IdentityData.empty.endorse("a").endorse(identity),
        "b" -> IdentityData.empty.endorse("b").endorse(identity),
        "c" -> IdentityData.empty.endorse("a").endorse("b")
      )

      val queryEngine = new InMemoryIdentityQueryEngine(map)

      val request = Get(withLedgerId(s"/identities/$identity/endorsements"))

      request ~> routes(queryEngine) ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        val result = json.as[Set[String]]
        result must be(endorsements)
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId(s"/identities/$identity/endorsements")
    }
  }

  "POST /identities" should {
    val pair = generateSigningKeyPair()
    val pair2 = generateSigningKeyPair()

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
        linkingIdentityPrivateKey: Option[SigningPrivateKey] = None,
        expectedResult: StatusCode = StatusCodes.Created
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

      val request = Post(withLedgerId("/identities"), jsonEntity(body))

      request ~> routes() ~> check {
        status must ===(expectedResult)

        val json = responseAs[JsValue]

        if (expectedResult == StatusCodes.Created) {
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
        } else {
          (json \ "errors").as[List[JsValue]] mustNot be(empty)
        }
      }
    }

    def testTransactionLinkType(
        linkingIdentityPrivateKey: Option[SigningPrivateKey],
        linkingIdentityPublicKey: SigningPublicKey,
        privateKey: SigningPrivateKey,
        expectedResult: StatusCode = StatusCodes.Created
    ): Assertion = {

      val privateKeyLinkHex = linkingIdentityPrivateKey.map(_.toByteString).map(toCleanHex)
      val publicKeyLinkHex = toCleanHex(linkingIdentityPublicKey.toByteString)
      val privateKeyHex = toCleanHex(privateKey.toByteString)
      val txType = "Link"
      val identity = "iohk"
      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "data": {
           |      "identity": "$identity",
           |      "key": "$publicKeyLinkHex"
           |    },
           |    "privateKey": "$privateKeyHex",
           |    "linkingIdentityPrivateKey": "${privateKeyLinkHex.getOrElse("")}"
           |
           |}
         """.stripMargin

      val request = Post(withLedgerId("/identities"), jsonEntity(body))

      request ~> routes() ~> check {
        status must ===(expectedResult)
        val json = responseAs[JsValue]
        if (expectedResult == StatusCodes.Created) {
          (json \ "type").as[String] must be(txType)
          (json \ "data" \ "key").as[String] must be(linkingIdentityPublicKey.toString)
          (json \ "data" \ "identity").as[String] must be(identity)
          (json \ "signature").as[String] mustNot be(empty)
          (json \ "linkingIdentitySignature").as[String] mustNot be(empty)
        } else {
          (json \ "errors").as[List[JsValue]] mustNot be(empty)
        }
      }
    }

    def testRevokeType(
        endorserIdentity: String,
        endorsedIdentity: String,
        privateKey: SigningPrivateKey,
        expectedResult: StatusCode = StatusCodes.Created
    ): Assertion = {
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
           |    "privateKey": "$privateKeyHex"
           |
           |}
         """.stripMargin

      val request = Post(withLedgerId("/identities"), jsonEntity(body))

      request ~> routes() ~> check {
        status must ===(StatusCodes.Created)
        val json = responseAs[JsValue]
        (json \ "type").as[String] must be(txType)
        (json \ "data" \ "endorserIdentity").as[String] must be(endorserIdentity)
        (json \ "data" \ "endorsedIdentity").as[String] must be(endorsedIdentity)
        (json \ "signature").as[String] mustNot be(empty)
      }
    }

    def testTransactionLinkCertificate(
        privateKey: SigningPrivateKey,
        certificatePrivateKey: SigningPrivateKey,
        expectedResult: StatusCode = StatusCodes.Created
    ): Assertion = {
      val pem = twoChainedCertsPEM
      val pemHex = toCleanHex(ByteString(twoChainedCertsPEM))
      val privateKeyLinkHex = toCleanHex(certificatePrivateKey.toByteString)
      val privateKeyHex = toCleanHex(privateKey.toByteString)
      val identity = "valid"
      val txType = "LinkCertificate"

      val body =
        s"""
           |{
           |    "type": "$txType",
           |    "data": {
           |      "linkingIdentity": "$identity",
           |      "pem": "$pemHex"
           |    },
           |    "privateKey": "$privateKeyHex",
           |    "linkingIdentityPrivateKey": "$privateKeyLinkHex"
           |
           |}
         """.stripMargin

      val request = Post(withLedgerId("/identities"), jsonEntity(body))

      request ~> routes() ~> check {
        status must ===(expectedResult)
        val json = responseAs[JsValue]
        if (expectedResult == StatusCodes.Created) {
          (json \ "type").as[String] must be(txType)
          (json \ "data" \ "pem").as[String] must be(pem)
          (json \ "data" \ "linkingIdentity").as[String] must be(identity)
          (json \ "signature").as[String] mustNot be(empty)
          (json \ "signatureFromCertificate").as[String] mustNot be(empty)
        } else {
          (json \ "errors").as[List[JsValue]] mustNot be(empty)
        }
      }
    }

    "be able to create identity claim transaction" in {
      testTransactionType("Claim", pair.`private`, identity = Some("iohk"), key = Some(pair.public))
    }

    "fail to create identity claim transaction when the keys don't correspond each other" in {
      testTransactionType(
        "Claim",
        pair.`private`,
        identity = Some("iohk"),
        key = Some(pair2.public),
        expectedResult = StatusCodes.BadRequest
      )
    }

    "be able to create identity link transaction" in {
      testTransactionLinkType(
        linkingIdentityPrivateKey = Option(pair.`private`),
        linkingIdentityPublicKey = pair.public,
        privateKey = pair2.`private`
      )
    }

    "fail to create identity link transaction when linkingIdentityPrivateKey is missing" in {
      testTransactionLinkType(
        linkingIdentityPrivateKey = None,
        linkingIdentityPublicKey = pair.public,
        privateKey = pair2.`private`,
        expectedResult = StatusCodes.BadRequest
      )
    }

    "fail to create identity link transaction when the keys don't correspond each other" in {
      testTransactionLinkType(
        Option(pair2.`private`),
        pair.public,
        pair2.`private`,
        expectedResult = StatusCodes.BadRequest
      )
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

    "fail to create identity grant transaction when the keys don't correspond each other" in {
      testTransactionType(
        "Grant",
        pair.`private`,
        grantedIdentity = Some("granted"),
        grantingIdentity = Some("granting"),
        grantedIdentityPublicKey = Some(pair2.public),
        linkingIdentityPrivateKey = Some(pair.`private`),
        expectedResult = StatusCodes.BadRequest
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
      testTransactionLinkCertificate(pair.`private`, toSigningPrivateKey(validCertPrivateKey).value)
    }

    "fail to create identity LinkCertificate transaction when the keys don't correspond each other" in {
      testTransactionLinkCertificate(pair2.`private`, pair.`private`, expectedResult = StatusCodes.BadRequest)
    }

    "return validation errors" in {
      val publicKeyHex = toCleanHex(pair.public.toByteString)
      val privateKeyHex = toCleanHex(pair.`private`.toByteString)
      val body =
        s"""
           |{
           |    "type": "none",
           |    "identity": 2,
           |    "data": "1$publicKeyHex",
           |    "privateKey": "2$privateKeyHex"
           |}
         """.stripMargin

      val request = Post(withLedgerId("/identities"), jsonEntity(body))

      request ~> routes() ~> check {
        status must ===(StatusCodes.BadRequest)

        val json = responseAs[JsValue]
        validateErrorResponse(json, 1)
      }
    }

    "return data errors" in {
      val privateKeyHex = toCleanHex(pair.`private`.toByteString)
      val body =
        s"""
           |{
           |    "type": "claim",
           |    "identity": "x",
           |    "data": {},
           |    "privateKey": "$privateKeyHex"
           |}
         """.stripMargin

      val request = Post(withLedgerId("/identities"), jsonEntity(body))

      request ~> routes() ~> check {
        status must ===(StatusCodes.BadRequest)

        val json = responseAs[JsValue]
        validateErrorResponse(json, 2)
      }
    }

    "return missing field errors" in {
      val request = Post(withLedgerId("/identities"), jsonEntity("{}"))

      request ~> routes() ~> check {
        status must ===(StatusCodes.BadRequest)

        val json = responseAs[JsValue]
        validateErrorResponse(json, 1)
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId(s"/identities", Option("{}"))
    }
  }

  def testUnknownLedgerId(path: String, body: Option[String] = None) = {
    val request = {
      body
        .map { bodyStr =>
          Post(s"/ledgers/unknown$path", jsonEntity(bodyStr))
        }
        .getOrElse {
          Get(s"/ledgers/unknown$path")
        }
    }

    request ~> routes() ~> check {
      status must ===(StatusCodes.BadRequest)

      val json = responseAs[JsValue]
      validateErrorResponse(json, 1)
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

object IdentitiesControllerSpec {
  class DummyNodeTransactionService[State, Tx <: Transaction[State], Q <: LedgerQuery[State]]
      extends NodeTransactionService[State, Tx, Q] {
    override def getQueryService(ledgerId: LedgerId): LedgerQueryService[State, Q] = ???

    override def receiveBlock(blEnvelope: Envelope[Block[State, Tx]]): Future[Either[ApplicationError, Unit]] = ???

    override def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]] = ???

    override def supportedLedgerIds: Set[LedgerId] = ???
  }

  class InMemoryIdentityQueryEngine(map: Map[String, IdentityPartition]) extends IdentityQueryEngine(null) {
    override def keys(): Set[String] = map.keys.toSet

    override def contains(partitionId: String): Boolean = map.contains(partitionId)

    override def get(partitionId: String): Option[IdentityPartition] = map.get(partitionId)
  }
}

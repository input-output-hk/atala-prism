package io.iohk.cef.frontend.controllers

import java.util.Base64

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.alexitc.playsonify.akka.PublicErrorRenderer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.models.{
  CreateChimericTransactionFragment,
  CreateChimericTransactionRequest,
  CreateNonSignableChimericTransactionFragment,
  CreateSignableChimericTransactionFragment,
  SubmitChimericTransactionRequest
}
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.UnsupportedLedgerException
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.query.chimeric.ChimericQuery
import io.iohk.crypto._
import org.mockito.Mockito.when
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec
import org.scalatest.mockito.MockitoSugar.mock
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.Future

class ChimericTransactionsControllerSpec extends WordSpec with ScalatestRouteTest {

  import Codecs._

  val ledgerId = "1"

  implicit val executionContext = system.dispatcher
  val signingKeyPair1 = generateSigningKeyPair()
  val signingKeyPair2 = generateSigningKeyPair()

  def encodeAddress(key: SigningPublicKey): String = Base64.getUrlEncoder.encodeToString(key.toByteString.toArray)

  "POST /chimeric-transactions" should {
    "allow to submit a Non Signable transaction" in {
      val (service, routes) = prepare()
      val fragments: Seq[CreateChimericTransactionFragment] = Seq(
        CreateNonSignableChimericTransactionFragment(Mint(value = Value(Map("USD" -> BigDecimal(200))))),
        CreateNonSignableChimericTransactionFragment(Mint(value = Value(Map("GBP" -> BigDecimal(9990))))),
        CreateNonSignableChimericTransactionFragment(
          Output(Value(Map("USD" -> BigDecimal(200))), signingKeyPair1.public)
        ),
        CreateNonSignableChimericTransactionFragment(
          Deposit(
            address = signingKeyPair1.public,
            value = Value(Map("GBP" -> BigDecimal(9990)))
          )
        ),
        CreateNonSignableChimericTransactionFragment(CreateCurrency(currency = "AUD"))
      )
      val entity = CreateChimericTransactionRequest(fragments = fragments)

      val json = Json.toJson(entity)
      val request = Post(withLedgerId("/chimeric-transactions"), json)

      val dummyChimericTx = ChimericTx(Seq())

      when(service.createChimericTransaction(entity)).thenReturn(Future.successful(Right(dummyChimericTx)))
      when(service.submitChimericTransaction(SubmitChimericTransactionRequest(Seq(), ledgerId)))
        .thenReturn(Future.successful(Right(())))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
      }
    }

    "allow to submit a Signable transaction" in {
      val (service, routes) = prepare()
      val fragments: Seq[CreateChimericTransactionFragment] = Seq(
        CreateSignableChimericTransactionFragment(
          Withdrawal(address = signingKeyPair1.public, value = Value(Map("MXN" -> BigDecimal(10))), nonce = 1),
          signingKeyPair1.`private`
        ),
        CreateSignableChimericTransactionFragment(
          Input(txOutRef = TxOutRef("txid", 0), value = Value(Map("CAD" -> BigDecimal(200)))),
          signingKeyPair2.`private`
        ),
        CreateNonSignableChimericTransactionFragment(
          Fee(Value(Map("MXN" -> BigDecimal(10), "CAD" -> BigDecimal(200))))
        )
      )
      val entity = CreateChimericTransactionRequest(fragments = fragments)

      val dummyChimericTx = ChimericTx(Seq())

      when(service.createChimericTransaction(entity)).thenReturn(Future.successful(Right(dummyChimericTx)))
      when(service.submitChimericTransaction(SubmitChimericTransactionRequest(Seq(), ledgerId)))
        .thenReturn(Future.successful(Right(())))

      val json = Json.toJson(entity)
      val request = Post(withLedgerId("/chimeric-transactions"), json)

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
      }
    }

    "allow to submit a Signable and Non Signable transaction" in {
      val (service, routes) = prepare()
      val fragments: Seq[CreateChimericTransactionFragment] = Seq(
        CreateSignableChimericTransactionFragment(
          Withdrawal(signingKeyPair1.public, value = Value(Map("MXN" -> BigDecimal(10))), nonce = 1),
          signingKeyPair1.`private`
        ),
        CreateSignableChimericTransactionFragment(
          Input(txOutRef = TxOutRef("txid", 0), value = Value(Map("CAD" -> BigDecimal(200)))),
          signingKeyPair2.`private`
        ),
        CreateNonSignableChimericTransactionFragment(Mint(value = Value(Map("USD" -> BigDecimal(200))))),
        CreateNonSignableChimericTransactionFragment(Fee(value = Value(Map("MXN" -> BigDecimal(10))))),
        CreateNonSignableChimericTransactionFragment(
          Output(Value(Map("CAD" -> BigDecimal(200))), signingKeyPair1.public)
        ),
        CreateNonSignableChimericTransactionFragment(
          Deposit(
            address = signingKeyPair1.public,
            value = Value(Map("USD" -> BigDecimal(200)))
          )
        ),
        CreateNonSignableChimericTransactionFragment(CreateCurrency(currency = "AUD"))
      )
      val entity = CreateChimericTransactionRequest(fragments = fragments)

      val dummyChimericTx = ChimericTx(Seq())

      when(service.createChimericTransaction(entity)).thenReturn(Future.successful(Right(dummyChimericTx)))
      when(service.submitChimericTransaction(SubmitChimericTransactionRequest(Seq(), ledgerId)))
        .thenReturn(Future.successful(Right(())))

      val json = Json.toJson(entity)
      val request = Post(withLedgerId("/chimeric-transactions"), json)

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
      }
    }

    "return missing top-level field errors" in {
      val (_, routes) = prepare()
      val request = Post(withLedgerId("/chimeric-transactions"), HttpEntity(ContentTypes.`application/json`, "{}"))

      request ~> routes ~> check {
        status must ===(StatusCodes.BadRequest)

        val json = responseAs[JsValue]
        validateErrorResponse(json, 1)
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId("/chimeric-transactions", Some("{}"))
    }
  }

  "GET /chimeric-transactions/currencies" should {
    "return the existing currency" in {
      val (service, routes) = prepare()
      val currencies = List("ADA", "BTC", "MXN")

      when(service.executeQuery(ledgerId, ChimericQuery.AllCurrencies))
        .thenReturn(Future.successful(Right(currencies.toSet)))

      val request = Get(withLedgerId("/chimeric-transactions/currencies"))

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "data").as[List[String]] must be(currencies)
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId("/chimeric-transactions/currencies")
    }
  }

  "GET /chimeric-transactions/currencies/:currency" should {
    "query an existing currency" in {
      val (service, routes) = prepare()

      when(service.executeQuery(ledgerId, ChimericQuery.CreatedCurrency("GBP")))
        .thenReturn(Future.successful(Right(Some(CurrencyQuery("GBP")))))

      val request = Get(withLedgerId("/chimeric-transactions/currencies/GBP"))

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "currency").as[String] must be("GBP")
      }
    }

    "query a non existing currency" in {
      val (service, routes) = prepare()

      when(service.executeQuery(ledgerId, ChimericQuery.CreatedCurrency("GBP")))
        .thenReturn(Future.successful(Right(None)))
      val request = Get(withLedgerId("/chimeric-transactions/currencies/GBP"))

      request ~> routes ~> check {
        status must ===(StatusCodes.NotFound)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("field-validation-error")
        (errors(0) \ "field").as[String] must be("currency")
        (errors(0) \ "message").as[String] must be("No results found")
      }
    }

    "handle a failed currency query" in {
      val (service, routes) = prepare()

      when(service.executeQuery(ledgerId, ChimericQuery.CreatedCurrency("GBP")))
        .thenReturn(Future.successful(Left(FakeApplicationError("something gone wrong"))))
      val request = Get(withLedgerId("/chimeric-transactions/currencies/GBP"))

      request ~> routes ~> check {
        status must ===(StatusCodes.InternalServerError)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("server-error")
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId("/chimeric-transactions/currencies/GBP")
    }
  }

  "GET chimeric-transactions/utxos/:utxo/balance" should {
    "query the balance of an existing utxo" in {
      val (service, routes) = prepare()

      when(service.executeQuery(ledgerId, ChimericQuery.UtxoBalance(TxOutRef("foo", 123))))
        .thenReturn(
          Future.successful(Right(Some(UtxoResult(Value(Map("GBP" -> BigDecimal(12))), signingKeyPair1.public))))
        )

      val request = Get(withLedgerId("/chimeric-transactions/utxos/foo(123)/balance"))

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "value" \ "GBP").as[BigDecimal] must be(BigDecimal(12))
      }
    }

    "query the balance of a non existing utxo" in {
      val (service, routes) = prepare()

      when(service.executeQuery(ledgerId, ChimericQuery.UtxoBalance(TxOutRef("foo", 123))))
        .thenReturn(Future.successful(Right(None)))

      val request = Get(withLedgerId("/chimeric-transactions/utxos/foo(123)/balance"))

      request ~> routes ~> check {
        status must ===(StatusCodes.NotFound)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("field-validation-error")
        (errors(0) \ "field").as[String] must be("utxo")
        (errors(0) \ "message").as[String] must be("No results found")
      }
    }

    "handle a failed utxo balance query" in {
      val (service, routes) = prepare()

      when(service.executeQuery(ledgerId, ChimericQuery.UtxoBalance(TxOutRef("foo", 123))))
        .thenReturn(Future.successful(Left(FakeApplicationError("something gone wrong"))))

      val request = Get(withLedgerId("/chimeric-transactions/utxos/foo(123)/balance"))

      request ~> routes ~> check {
        status must ===(StatusCodes.InternalServerError)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("server-error")
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId("/chimeric-transactions/utxos/foo(123)/balance")
    }
  }

  "GET /chimeric-transactions/addresses/:address/balance" should {
    "query the balance of an existing address" in {
      val (service, routes) = prepare()
      val urlAddress = encodeAddress(signingKeyPair1.public)

      when(service.executeQuery(ledgerId, ChimericQuery.AddressBalance(signingKeyPair1.public)))
        .thenReturn(Future.successful(Right(Some(AddressResult(Value(Map("GBP" -> BigDecimal(12))))))))

      val request = Get(withLedgerId(s"/chimeric-transactions/addresses/$urlAddress/balance"))

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "value" \ "GBP").as[BigDecimal] must be(BigDecimal(12))
      }
    }

    "query the balance of a non existing address" in {
      val (service, routes) = prepare()
      val urlAddress = encodeAddress(signingKeyPair1.public)

      when(service.executeQuery(ledgerId, ChimericQuery.AddressBalance(signingKeyPair1.public)))
        .thenReturn(Future.successful(Right(None)))

      val request = Get(withLedgerId(s"/chimeric-transactions/addresses/$urlAddress/balance"))

      request ~> routes ~> check {
        status must ===(StatusCodes.NotFound)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("field-validation-error")
        (errors(0) \ "field").as[String] must be("address")
        (errors(0) \ "message").as[String] must be("No results found")
      }
    }

    val urlAddress = encodeAddress(signingKeyPair1.public)
    "handle a failed address balance query" in {
      val (service, routes) = prepare()

      when(service.executeQuery(ledgerId, ChimericQuery.AddressBalance(signingKeyPair1.public)))
        .thenReturn(Future.successful(Left(FakeApplicationError("something gone wrong"))))

      val request = Get(withLedgerId(s"/chimeric-transactions/addresses/$urlAddress/balance"))

      request ~> routes ~> check {
        status must ===(StatusCodes.InternalServerError)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("server-error")
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId(s"/chimeric-transactions/addresses/$urlAddress/balance")
    }
  }

  "GET /chimeric-transactions/addresses/:address/nonce" should {
    "query the nonce of an existing address" in {
      val (service, routes) = prepare()
      val urlAddress = encodeAddress(signingKeyPair1.public)

      when(service.executeQuery(ledgerId, ChimericQuery.AddressNonce(signingKeyPair1.public)))
        .thenReturn(Future.successful(Right(Some(NonceResult(123)))))

      val request = Get(withLedgerId(s"/chimeric-transactions/addresses/$urlAddress/nonce"))

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "nonce").as[Int] must be(123)
      }
    }

    "query the nonce of a non existing address" in {
      val (service, routes) = prepare()
      val urlAddress = encodeAddress(signingKeyPair1.public)

      when(service.executeQuery(ledgerId, ChimericQuery.AddressNonce(signingKeyPair1.public)))
        .thenReturn(Future.successful(Right(None)))

      val request = Get(withLedgerId(s"/chimeric-transactions/addresses/$urlAddress/nonce"))

      request ~> routes ~> check {
        status must ===(StatusCodes.NotFound)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("field-validation-error")
        (errors(0) \ "field").as[String] must be("address")
        (errors(0) \ "message").as[String] must be("No results found")
      }
    }

    val urlAddress = encodeAddress(signingKeyPair1.public)
    "handle a failed address nonce query" in {
      val (service, routes) = prepare()

      when(service.executeQuery(ledgerId, ChimericQuery.AddressNonce(signingKeyPair1.public)))
        .thenReturn(Future.successful(Left(FakeApplicationError("something gone wrong"))))

      val request = Get(withLedgerId(s"/chimeric-transactions/addresses/$urlAddress/nonce"))

      request ~> routes ~> check {
        status must ===(StatusCodes.InternalServerError)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("server-error")
      }
    }

    "fail on unknown ledger id" in {
      testUnknownLedgerId(s"/chimeric-transactions/addresses/$urlAddress/nonce")
    }
  }

  def prepare(): (ChimericTransactionService, Route) = {
    val service = mock[ChimericTransactionService]
    val api = new ChimericTransactionsController(service)
    val routes = api.routes
    (service, routes)
  }

  def validateErrorResponse(json: JsValue, size: Int) = {
    val errors = (json \ "errors").as[List[JsValue]]
    errors.size must be(size)
    errors.foreach { error =>
      (error \ "type").as[String] must be(PublicErrorRenderer.FieldValidationErrorType)
      (error \ "message").as[String] mustNot be(empty)
      (error \ "field").as[String] mustNot be(empty)
    }
  }

  def testUnknownLedgerId(path: String, body: Option[String] = None) = {
    val request = {
      body
        .map { bodyStr =>
          Post(s"/ledgers/unknown$path", HttpEntity(ContentTypes.`application/json`, bodyStr))
        }
        .getOrElse {
          Get(s"/ledgers/unknown$path")
        }
    }

    import org.mockito.ArgumentMatchers._

    val service = mock[ChimericTransactionService]
    when(service.executeQuery(anyString, any[ChimericQuery]))
      .thenReturn(Future.failed(UnsupportedLedgerException("ledgerId")))
    val api = new ChimericTransactionsController(service)
    val routes = api.routes
    request ~> routes ~> check {
      status must ===(StatusCodes.BadRequest)

      val json = responseAs[JsValue]
      validateErrorResponse(json, 1)
    }
  }

  private def withLedgerId(path: String) = s"/ledgers/$ledgerId$path"

  case class FakeApplicationError(message: String) extends ApplicationError
}

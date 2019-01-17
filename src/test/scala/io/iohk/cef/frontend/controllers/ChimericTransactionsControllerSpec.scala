package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.alexitc.playsonify.akka.PublicErrorRenderer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.transactionservice.NodeTransactionService
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.models.{
  CreateChimericTransactionFragment,
  CreateChimericTransactionRequest,
  CreateNonSignableChimericTransactionFragment,
  CreateSignableChimericTransactionFragment
}
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.chimeric._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsValue, Json}
import io.iohk.cef.error.ApplicationError

import scala.concurrent.Future

class ChimericTransactionsControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  import Codecs._

  val nodeTransactionService = mock[NodeTransactionService[ChimericStateResult, ChimericTx]]
  when(nodeTransactionService.receiveTransaction(any())).thenReturn(Future.successful(Right(())))

  implicit val executionContext = system.dispatcher
  val service = new ChimericTransactionService(nodeTransactionService, null)
  val api = new ChimericTransactionsController(service)
  val routes = api.routes
  val signingKeyPair1 = generateSigningKeyPair()
  val signingKeyPair2 = generateSigningKeyPair()

  "POST /chimeric-transactions" should {
    "allow to submit a Non Signable transaction" in {
      val fragments: Seq[CreateChimericTransactionFragment] = Seq(
        CreateNonSignableChimericTransactionFragment(Mint(value = Value(Map("USD" -> BigDecimal(200))))),
        CreateNonSignableChimericTransactionFragment(Fee(value = Value(Map("GBP" -> BigDecimal(9990))))),
        CreateNonSignableChimericTransactionFragment(
          Output(Value(Map("EUR" -> BigDecimal(5))), signingKeyPair1.public)
        ),
        CreateNonSignableChimericTransactionFragment(
          Deposit(
            address = "another",
            value = Value(Map("PLN" -> BigDecimal(100000))),
            signingPublicKey = signingKeyPair1.public
          )
        ),
        CreateNonSignableChimericTransactionFragment(CreateCurrency(currency = "AUD"))
      )
      val entity = CreateChimericTransactionRequest(fragments = fragments, "1")

      val json = Json.toJson(entity)
      val request = Post("/chimeric-transactions", json)

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
      }
    }

    "allow to submit a Signable transaction" in {
      val fragments: Seq[CreateChimericTransactionFragment] = Seq(
        CreateSignableChimericTransactionFragment(
          Withdrawal(address = "dummy", value = Value(Map("MXN" -> BigDecimal(10))), nonce = 1),
          signingKeyPair1.`private`
        ),
        CreateSignableChimericTransactionFragment(
          Input(txOutRef = TxOutRef("txid", 0), value = Value(Map("CAD" -> BigDecimal(200)))),
          signingKeyPair2.`private`
        )
      )
      val entity = CreateChimericTransactionRequest(fragments = fragments, "1")

      val json = Json.toJson(entity)
      val request = Post("/chimeric-transactions", json)

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
      }
    }

    "allow to submit a Signable and Non Signable transaction" in {
      val fragments: Seq[CreateChimericTransactionFragment] = Seq(
        CreateSignableChimericTransactionFragment(
          Withdrawal(address = "dummy", value = Value(Map("MXN" -> BigDecimal(10))), nonce = 1),
          signingKeyPair1.`private`
        ),
        CreateSignableChimericTransactionFragment(
          Input(txOutRef = TxOutRef("txid", 0), value = Value(Map("CAD" -> BigDecimal(200)))),
          signingKeyPair2.`private`
        ),
        CreateNonSignableChimericTransactionFragment(Mint(value = Value(Map("USD" -> BigDecimal(200))))),
        CreateNonSignableChimericTransactionFragment(Fee(value = Value(Map("GBP" -> BigDecimal(9990))))),
        CreateNonSignableChimericTransactionFragment(
          Output(Value(Map("EUR" -> BigDecimal(5))), signingKeyPair1.public)
        ),
        CreateNonSignableChimericTransactionFragment(
          Deposit(
            address = "another",
            value = Value(Map("PLN" -> BigDecimal(100000))),
            signingPublicKey = signingKeyPair1.public
          )
        ),
        CreateNonSignableChimericTransactionFragment(CreateCurrency(currency = "AUD"))
      )
      val entity = CreateChimericTransactionRequest(fragments = fragments, "1")

      val json = Json.toJson(entity)
      val request = Post("/chimeric-transactions", json)

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
      }
    }

    "return missing top-level field errors" in {
      val request = Post("/chimeric-transactions", HttpEntity(ContentTypes.`application/json`, "{}"))

      request ~> routes ~> check {
        status must ===(StatusCodes.BadRequest)

        val json = responseAs[JsValue]
        validateErrorResponse(json, 2)
      }
    }

    "query an existing currency" in {
      val (service, routes) = prepare()
      when(service.queryCreatedCurrency("GBP")).thenReturn(Future.successful(Right(Some(CurrencyQuery("GBP")))))

      val request = Get("/chimeric-transactions/currencies/GBP")

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "currency").as[String] must be("GBP")
      }
    }

    "query a non existing currency" in {
      val (service, routes) = prepare()
      when(service.queryCreatedCurrency("GBP")).thenReturn(Future.successful(Right(None)))
      val request = Get("/chimeric-transactions/currencies/GBP")

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
      when(service.queryCreatedCurrency("GBP"))
        .thenReturn(Future.successful(Left(FakeApplicationError("something gone wrong"))))
      val request = Get("/chimeric-transactions/currencies/GBP")

      request ~> routes ~> check {
        status must ===(StatusCodes.InternalServerError)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("server-error")
      }
    }

    "query the balance of an existing utxo" in {
      val (service, routes) = prepare()
      when(service.queryUtxoBalance(TxOutRef("foo", 123)))
        .thenReturn(Future.successful(Right(Some(UtxoResult(Value(Map("GBP" -> BigDecimal(12))), None)))))

      val request = Get("/chimeric-transactions/utxos/foo(123)/balance")

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "value" \ "m" \ "GBP").as[BigDecimal] must be(BigDecimal(12))
      }
    }

    "query the balance of a non existing utxo" in {
      val (service, routes) = prepare()
      when(service.queryUtxoBalance(TxOutRef("foo", 123))).thenReturn(Future.successful(Right(None)))

      val request = Get("/chimeric-transactions/utxos/foo(123)/balance")

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
      when(service.queryUtxoBalance(TxOutRef("foo", 123)))
        .thenReturn(Future.successful(Left(FakeApplicationError("something gone wrong"))))

      val request = Get("/chimeric-transactions/utxos/foo(123)/balance")

      request ~> routes ~> check {
        status must ===(StatusCodes.InternalServerError)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("server-error")
      }
    }

    "query the balance of an existing address" in {
      val (service, routes) = prepare()
      when(service.queryAddressBalance("myaddress"))
        .thenReturn(Future.successful(Right(Some(AddressResult(Value(Map("GBP" -> BigDecimal(12))), None)))))

      val request = Get("/chimeric-transactions/addresses/myaddress/balance")

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "value" \ "m" \ "GBP").as[BigDecimal] must be(BigDecimal(12))
      }
    }

    "query the balance of a non existing address" in {
      val (service, routes) = prepare()
      when(service.queryAddressBalance("myaddress")).thenReturn(Future.successful(Right(None)))

      val request = Get("/chimeric-transactions/addresses/myaddress/balance")

      request ~> routes ~> check {
        status must ===(StatusCodes.NotFound)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("field-validation-error")
        (errors(0) \ "field").as[String] must be("address")
        (errors(0) \ "message").as[String] must be("No results found")
      }
    }

    "handle a failed address balance query" in {
      val (service, routes) = prepare()
      when(service.queryAddressBalance("myaddress"))
        .thenReturn(Future.successful(Left(FakeApplicationError("something gone wrong"))))

      val request = Get("/chimeric-transactions/addresses/myaddress/balance")

      request ~> routes ~> check {
        status must ===(StatusCodes.InternalServerError)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("server-error")
      }
    }

    "query the nonce of an existing address" in {
      val (service, routes) = prepare()
      when(service.queryAddressNonce("myaddress")).thenReturn(Future.successful(Right(Some(NonceResult(123)))))

      val request = Get("/chimeric-transactions/addresses/myaddress/nonce")

      request ~> routes ~> check {
        status must ===(StatusCodes.OK)

        val json = responseAs[JsValue]
        (json \ "nonce").as[Int] must be(123)
      }
    }

    "query the nonce of a non existing address" in {
      val (service, routes) = prepare()
      when(service.queryAddressNonce("myaddress")).thenReturn(Future.successful(Right(None)))

      val request = Get("/chimeric-transactions/addresses/myaddress/nonce")

      request ~> routes ~> check {
        status must ===(StatusCodes.NotFound)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("field-validation-error")
        (errors(0) \ "field").as[String] must be("address")
        (errors(0) \ "message").as[String] must be("No results found")
      }
    }

    "handle a failed address nonce query" in {
      val (service, routes) = prepare()
      when(service.queryAddressNonce("myaddress"))
        .thenReturn(Future.successful(Left(FakeApplicationError("something gone wrong"))))

      val request = Get("/chimeric-transactions/addresses/myaddress/nonce")

      request ~> routes ~> check {
        status must ===(StatusCodes.InternalServerError)
        val json = responseAs[JsValue]
        val errors = (json \ "errors").as[List[JsValue]]
        (errors(0) \ "type").as[String] must be("server-error")
      }
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

  case class FakeApplicationError(message: String) extends ApplicationError
}

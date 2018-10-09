package io.iohk.cef.frontend.client

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.iohk.cef.core.NodeCore
import io.iohk.cef.crypto._
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

import scala.concurrent.Future

class ChimericServiceApiSpec extends WordSpec with MustMatchers with ScalaFutures with ScalatestRouteTest {

  val nodeCore = mock[NodeCore[ChimericStateResult, ChimericBlockHeader, ChimericTx]]
  when(nodeCore.receiveTransaction(any())).thenReturn(Future.successful(Right(())))

  implicit val executionContext = system.dispatcher
  val service = new ChimericTransactionService(nodeCore)
  val api = new ChimericServiceApi(service)
  val routes = api.create
  val signingKeyPair1 = generateSigningKeyPair()
  val signingKeyPair2 = generateSigningKeyPair()

  "POST /chimeric-transactions" should {
    "allow to submit a Non Signable transaction" in {

      val fragments: Seq[CreateChimericTransactionFragment] = Seq(
        CreateNonSignableChimericTransactionFragment(Mint(value = Value(Map("USD" -> BigDecimal(200))))),
        CreateNonSignableChimericTransactionFragment(Fee(value = Value(Map("GBP" -> BigDecimal(9990))))),
        CreateNonSignableChimericTransactionFragment(
          Output(Value(Map("EUR" -> BigDecimal(5))), signingKeyPair1.public)),
        CreateNonSignableChimericTransactionFragment(
          Deposit(
            address = "another",
            value = Value(Map("PLN" -> BigDecimal(100000))),
            signingPublicKey = signingKeyPair1.public)),
        CreateNonSignableChimericTransactionFragment(CreateCurrency(currency = "AUD"))
      )
      val entity = CreateChimericTransactionRequest(fragments = fragments, 1)

      val json = Marshal(entity).to[MessageEntity].futureValue
      val request = Post("/chimeric-transactions", json)

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
      }
    }
  }

  "allow to submit a Signable transaction" in {

    val fragments: Seq[CreateChimericTransactionFragment] = Seq(
      CreateSignableChimericTransactionFragment(
        Withdrawal(address = "dummy", value = Value(Map("MXN" -> BigDecimal(10))), nonce = 1),
        signingKeyPair1.`private`),
      CreateSignableChimericTransactionFragment(
        Input(txOutRef = TxOutRef("txid", 0), value = Value(Map("CAD" -> BigDecimal(200)))),
        signingKeyPair2.`private`)
    )
    val entity = CreateChimericTransactionRequest(fragments = fragments, 1)

    val json = Marshal(entity).to[MessageEntity].futureValue
    val request = Post("/chimeric-transactions", json)

    request ~> routes ~> check {
      status must ===(StatusCodes.Created)
    }
  }

  "allow to submit a Signable and Non Signable transaction" in {

    val fragments: Seq[CreateChimericTransactionFragment] = Seq(
      CreateSignableChimericTransactionFragment(
        Withdrawal(address = "dummy", value = Value(Map("MXN" -> BigDecimal(10))), nonce = 1),
        signingKeyPair1.`private`),
      CreateSignableChimericTransactionFragment(
        Input(txOutRef = TxOutRef("txid", 0), value = Value(Map("CAD" -> BigDecimal(200)))),
        signingKeyPair2.`private`),
      CreateNonSignableChimericTransactionFragment(Mint(value = Value(Map("USD" -> BigDecimal(200))))),
      CreateNonSignableChimericTransactionFragment(Fee(value = Value(Map("GBP" -> BigDecimal(9990))))),
      CreateNonSignableChimericTransactionFragment(Output(Value(Map("EUR" -> BigDecimal(5))), signingKeyPair1.public)),
      CreateNonSignableChimericTransactionFragment(
        Deposit(
          address = "another",
          value = Value(Map("PLN" -> BigDecimal(100000))),
          signingPublicKey = signingKeyPair1.public)),
      CreateNonSignableChimericTransactionFragment(CreateCurrency(currency = "AUD"))
    )
    val entity = CreateChimericTransactionRequest(fragments = fragments, 1)

    val json = Marshal(entity).to[MessageEntity].futureValue
    val request = Post("/chimeric-transactions", json)

    request ~> routes ~> check {
      status must ===(StatusCodes.Created)
    }
  }
}

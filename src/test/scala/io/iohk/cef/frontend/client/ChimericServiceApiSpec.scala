package io.iohk.cef.frontend.client

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.iohk.cef.crypto._
import io.iohk.cef.core.NodeCore
import io.iohk.cef.frontend.models.ChimericTransactionRequest
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
  val signingKeyPair = generateSigningKeyPair()
  "POST /chimeric-transactions" should {
    "allow to submit a transaction" in {
      val fragments: List[ChimericTxFragment] = List(
        Withdrawal(address = "dummy", value = Value(Map("MXN" -> BigDecimal(10))), nonce = 1),
        Mint(value = Value(Map("USD" -> BigDecimal(200)))),
        Input(txOutRef = TxOutRef("txid", 0), value = Value(Map("CAD" -> BigDecimal(200)))),
        Fee(value = Value(Map("GBP" -> BigDecimal(9990)))),
        Output(Value(Map("EUR" -> BigDecimal(5))), signingKeyPair.public),
        Deposit(
          address = "another",
          value = Value(Map("PLN" -> BigDecimal(100000))),
          signingPublicKey = signingKeyPair.public),
        CreateCurrency(currency = "AUD")
      )
      val transaction = ChimericTx(fragments)
      val entity = ChimericTransactionRequest(transaction, 1)
      val json = Marshal(entity).to[MessageEntity].futureValue

      val request = Post("/chimeric-transactions", json)

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
      }
    }
  }
}

package io.iohk.node.cardano.wallet

import com.softwaremill.sttp.StringBody
import com.softwaremill.sttp.testing.SttpBackendStub
import io.circe.parser._
import io.iohk.node.cardano.models.{Address, Lovelace}
import io.iohk.node.cardano.models.{Payment, TransactionId, WalletId}
import io.iohk.node.cardano.wallet.CardanoWalletApiClient.{CardanoWalletError, ErrorResponse}
import io.iohk.node.cardano.wallet.api.ApiClient
import org.scalatest.EitherValues._
import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global

class CardanoWalletApiClientSpec extends WordSpec with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(30, Millis))

  private val config = ApiClient.Config("localhost", 8090)

  "postTransaction" should {
    val walletId = WalletId.from("bf098c001609ad7b76a0239e27f2a6bf9f09fd71").value
    val payment = Payment(
      Address(
        "2cWKMJemoBakZBR9TG2YAmxxtJpyvBqv31yWuHjUWpjbc24XbxiLytuzxSdyMtrbCfGmb"
      ),
      Lovelace(42000000)
    )
    val passphrase = "Secure Passphrase"
    val expectedPath = s"v2/byron-wallets/$walletId/transactions"
    val expectedJsonRequest = readResource("postTransaction_request.json")

    "post a new transaction" in {
      val client =
        newClient(expectedPath, expectedJsonRequest, 200, readResource("postTransaction_success_response.json"))

      val transaction = client.postTransaction(walletId, List(payment), passphrase).value.futureValue.right.value

      transaction must be(
        TransactionId.from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1").value
      )
    }

    "fail on server error" in {
      val client =
        newClient(expectedPath, expectedJsonRequest, 400, createJsonErrorResponse("not_found", "Bad request"))

      val error = client.postTransaction(walletId, List(payment), passphrase).value.futureValue.left.value

      error must be(ErrorResponse(expectedPath, CardanoWalletError("not_found", "Bad request")))
    }
  }

  private def newClient(
      expectedPath: String,
      expectedJsonRequest: String,
      responseCode: Int,
      responseBody: String
  ): CardanoWalletApiClient = {
    val backend = SttpBackendStub.asynchronousFuture
      .whenRequestMatches(request =>
        request.uri.host == config.host && request.uri.port.value == config.port && request.uri.path
          .mkString("/") == expectedPath && sameJson(request.body.asInstanceOf[StringBody].s, expectedJsonRequest)
      )
      .thenRespondWithCode(responseCode, responseBody)
    new ApiClient(config)(backend, global)
  }

  private def sameJson(a: String, b: String): Boolean = {
    parse(a).right.value == parse(b).right.value
  }

  private def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(s"cardano/wallet/$resource").mkString
    } catch {
      case _: Throwable => throw new RuntimeException(s"Resource $resource not found")
    }
  }

  private def createJsonErrorResponse(errorCode: String, message: String): String = {
    s"""
       |{
       |  "code": "$errorCode",
       |  "message": "$message"
       |}
     """.stripMargin
  }
}

package io.iohk.atala.prism.node.cardano.wallet

import io.circe.Json
import io.iohk.atala.prism.models.{TransactionDetails, TransactionId, TransactionStatus}
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient.{CardanoWalletError, ErrorResponse}
import io.iohk.atala.prism.node.cardano.wallet.testing.FakeCardanoWalletApiClient
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

class CardanoWalletApiClientSpec extends AnyWordSpec with ScalaFutures {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(30, Millis))
  implicit def ec: ExecutionContext = ExecutionContext.global

  private val walletId = WalletId.from("bf098c001609ad7b76a0239e27f2a6bf9f09fd71").value

  "postTransaction" should {
    val payment = Payment(
      Address(
        "2cWKMJemoBakZBR9TG2YAmxxtJpyvBqv31yWuHjUWpjbc24XbxiLytuzxSdyMtrbCfGmb"
      ),
      Lovelace(42000000)
    )
    val metadata = TransactionMetadata(Json.obj("0" -> Json.fromString("0x1234567890abcdef")))
    val passphrase = "Secure Passphrase"
    val expectedPath = s"v2/wallets/$walletId/transactions"
    val expectedJsonRequest = readResource("postTransaction_request.json")

    "post a new transaction" in {
      val client =
        FakeCardanoWalletApiClient.Success(
          expectedPath,
          expectedJsonRequest,
          readResource("postTransaction_success_response.json")
        )

      val transaction =
        client.postTransaction(walletId, List(payment), Some(metadata), passphrase).value.futureValue.right.value

      transaction must be(
        TransactionId.from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1").value
      )
    }

    "fail on server error" in {
      val client =
        FakeCardanoWalletApiClient.Fail(
          expectedPath,
          expectedJsonRequest,
          "not_found",
          "Bad request"
        )

      val error =
        client.postTransaction(walletId, List(payment), Some(metadata), passphrase).value.futureValue.left.value

      error must be(ErrorResponse(expectedPath, CardanoWalletError("not_found", "Bad request")))
    }
  }

  "getTransaction" should {
    val transactionId = TransactionId.from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1").value
    val expectedPath = s"v2/wallets/$walletId/transactions/$transactionId"

    "get transaction details" in {
      val client =
        FakeCardanoWalletApiClient.Success(expectedPath, "", readResource("getTransaction_success_response.json"))

      val transactionDetails = client.getTransaction(walletId, transactionId).value.futureValue.right.value

      transactionDetails must be(TransactionDetails(transactionId, TransactionStatus.InLedger))
    }

    "fail on server error" in {
      val client =
        FakeCardanoWalletApiClient.Fail(
          expectedPath,
          "",
          "not_found",
          "Bad request"
        )

      val error = client.getTransaction(walletId, transactionId).value.futureValue.left.value

      error must be(ErrorResponse(expectedPath, CardanoWalletError("not_found", "Bad request")))
    }
  }

  "deleteTransaction" should {
    val transactionId = TransactionId.from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1").value
    val expectedPath = s"v2/wallets/$walletId/transactions/$transactionId"

    "delete a transaction" in {
      val client = FakeCardanoWalletApiClient.Success(expectedPath, "", "")

      client.deleteTransaction(walletId, transactionId).value.futureValue.right.value
    }

    "fail on server error" in {
      val client =
        FakeCardanoWalletApiClient.Fail(
          expectedPath,
          "",
          "not_found",
          "Bad request"
        )

      val error = client.deleteTransaction(walletId, transactionId).value.futureValue.left.value

      error must be(ErrorResponse(expectedPath, CardanoWalletError("not_found", "Bad request")))
    }
  }

  private def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(s"cardano/wallet/$resource").mkString
    } catch {
      case _: Throwable => throw new RuntimeException(s"Resource $resource not found")
    }
  }
}

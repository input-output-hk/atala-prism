package io.iohk.atala.prism.node.cardano.wallet

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.scalatest.EitherMatchers._
import io.circe.Json
import io.iohk.atala.prism.node.models.{TransactionDetails, TransactionId, TransactionStatus}
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient.{CardanoWalletError, ErrorResponse, EstimatedFee}
import io.iohk.atala.prism.node.cardano.wallet.testing.FakeCardanoWalletApiClient
import io.iohk.atala.prism.node.models.WalletStatus
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import sttp.model.Header

import scala.concurrent.ExecutionContext

class CardanoWalletApiClientSpec extends AnyWordSpec with ScalaFutures {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(30, Millis))
  implicit def ec: ExecutionContext = ExecutionContext.global

  private val walletId =
    WalletId.from("bf098c001609ad7b76a0239e27f2a6bf9f09fd71").value

  "estimateTransactionFee" should {
    val payment = Payment(
      Address(
        "2cWKMJemoBakZBR9TG2YAmxxtJpyvBqv31yWuHjUWpjbc24XbxiLytuzxSdyMtrbCfGmb"
      ),
      Lovelace(42000000)
    )
    val metadata = TransactionMetadata(
      Json.obj("0" -> Json.fromString("0x1234567890abcdef"))
    )
    val expectedPath = s"v2/wallets/$walletId/payment-fees"
    val expectedJsonRequest =
      readResource("estimateTransactionFee_request.json")

    "estimate the fee of a transaction" in {
      val client =
        FakeCardanoWalletApiClient.Success[IO](
          expectedPath,
          expectedJsonRequest,
          readResource("estimateTransactionFee_success_response.json")
        )

      val estimatedFee =
        client
          .estimateTransactionFee(walletId, List(payment), Some(metadata))
          .unsafeToFuture()
          .futureValue
          .toOption
          .value

      estimatedFee must be(
        EstimatedFee(min = Lovelace(133713), max = Lovelace(1000000))
      )
    }

    "estimate the fee of a transaction with header" in {
      val client =
        FakeCardanoWalletApiClient.Success[IO](
          expectedPath,
          expectedJsonRequest,
          readResource("estimateTransactionFee_success_response.json"),
          Some(Header("RoutingHeaderName", "RoutingHeaderValue"))
        )

      val estimatedFee =
        client
          .estimateTransactionFee(walletId, List(payment), Some(metadata))
          .unsafeToFuture()
          .futureValue
          .toOption
          .value

      estimatedFee must be(
        EstimatedFee(min = Lovelace(133713), max = Lovelace(1000000))
      )
    }

    "fail on server error" in {
      val client =
        FakeCardanoWalletApiClient.Fail[IO](
          expectedPath,
          expectedJsonRequest,
          "not_found",
          "Bad request"
        )

      val error =
        client
          .estimateTransactionFee(walletId, List(payment), Some(metadata))
          .unsafeToFuture()
          .futureValue
          .left
          .value

      error must be(
        ErrorResponse(
          expectedPath,
          CardanoWalletError("not_found", "Bad request")
        )
      )
    }
  }

  "postTransaction" should {
    val payment = Payment(
      Address(
        "2cWKMJemoBakZBR9TG2YAmxxtJpyvBqv31yWuHjUWpjbc24XbxiLytuzxSdyMtrbCfGmb"
      ),
      Lovelace(42000000)
    )
    val metadata = TransactionMetadata(
      Json.obj("0" -> Json.fromString("0x1234567890abcdef"))
    )
    val passphrase = "Secure Passphrase"
    val expectedPath = s"v2/wallets/$walletId/transactions"
    val expectedJsonRequest = readResource("postTransaction_request.json")

    "post a new transaction with no header" in {
      val client =
        FakeCardanoWalletApiClient.Success[IO](
          expectedPath,
          expectedJsonRequest,
          readResource("postTransaction_success_response.json")
        )

      val transaction =
        client
          .postTransaction(walletId, List(payment), Some(metadata), passphrase)
          .unsafeToFuture()
          .futureValue

      transaction must beRight(
        TransactionId
          .from(
            "1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1"
          )
          .value
      )
    }

    "post a new transaction with header" in {
      val client =
        FakeCardanoWalletApiClient.Success[IO](
          expectedPath,
          expectedJsonRequest,
          readResource("postTransaction_success_response.json"),
          Some(Header("RoutingHeaderName", "RoutingHeaderValue"))
        )

      val transaction =
        client
          .postTransaction(walletId, List(payment), Some(metadata), passphrase)
          .unsafeToFuture()
          .futureValue

      transaction must beRight(
        TransactionId
          .from(
            "1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1"
          )
          .value
      )
    }

    "fail on server error" in {
      val client =
        FakeCardanoWalletApiClient.Fail[IO](
          expectedPath,
          expectedJsonRequest,
          "not_found",
          "Bad request"
        )

      val error =
        client
          .postTransaction(walletId, List(payment), Some(metadata), passphrase)
          .unsafeToFuture()
          .futureValue
          .left
          .value

      error must be(
        ErrorResponse(
          expectedPath,
          CardanoWalletError("not_found", "Bad request")
        )
      )
    }
  }

  "getTransaction" should {
    val transactionId = TransactionId
      .from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
      .value
    val expectedPath = s"v2/wallets/$walletId/transactions/$transactionId"

    "get transaction details" in {
      val client =
        FakeCardanoWalletApiClient.Success[IO](
          expectedPath,
          "",
          readResource("getTransaction_success_response.json")
        )

      val transactionDetails = client
        .getTransaction(walletId, transactionId)
        .unsafeToFuture()
        .futureValue

      transactionDetails must beRight(
        TransactionDetails(transactionId, TransactionStatus.InLedger)
      )
    }

    "get transaction details with header" in {
      val client =
        FakeCardanoWalletApiClient.Success[IO](
          expectedPath,
          "",
          readResource("getTransaction_success_response.json"),
          Some(Header("RoutingHeaderName", "RoutingHeaderValue"))
        )

      val transactionDetails = client
        .getTransaction(walletId, transactionId)
        .unsafeToFuture()
        .futureValue

      transactionDetails must beRight(
        TransactionDetails(transactionId, TransactionStatus.InLedger)
      )
    }

    "fail on server error" in {
      val client =
        FakeCardanoWalletApiClient.Fail[IO](
          expectedPath,
          "",
          "not_found",
          "Bad request"
        )

      val error = client
        .getTransaction(walletId, transactionId)
        .unsafeToFuture()
        .futureValue
        .left
        .value

      error must be(
        ErrorResponse(
          expectedPath,
          CardanoWalletError("not_found", "Bad request")
        )
      )
    }
  }

  "deleteTransaction" should {
    val transactionId = TransactionId
      .from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
      .value
    val expectedPath = s"v2/wallets/$walletId/transactions/$transactionId"

    "delete a transaction" in {
      val client = FakeCardanoWalletApiClient.Success[IO](expectedPath, "", "")

      client
        .deleteTransaction(walletId, transactionId)
        .unsafeToFuture()
        .futureValue
    }

    "delete a transaction with header" in {
      val client = FakeCardanoWalletApiClient
        .Success[IO](expectedPath, "", "", Some(Header("RoutingHeaderName", "RoutingHeaderValue")))

      client
        .deleteTransaction(walletId, transactionId)
        .unsafeToFuture()
        .futureValue
    }

    "fail on server error" in {
      val client =
        FakeCardanoWalletApiClient.Fail[IO](
          expectedPath,
          "",
          "not_found",
          "Bad request"
        )

      val error = client
        .deleteTransaction(walletId, transactionId)
        .unsafeToFuture()
        .futureValue
        .left
        .value

      error must be(
        ErrorResponse(
          expectedPath,
          CardanoWalletError("not_found", "Bad request")
        )
      )
    }
  }

  "getWallet" should {
    val expectedPath = s"v2/wallets/$walletId"

    "return available funds and state data" in {
      val client = FakeCardanoWalletApiClient
        .Success[IO](expectedPath, "", readResource("getWallet.json"))

      val result = client.getWallet(walletId).unsafeToFuture().futureValue
      result.isRight mustBe true

      val Right(data) = result
      data.balance.available mustBe BigInt(42000000)
      data.state.status mustBe WalletStatus.Ready
    }

    "return available funds and state data with header" in {
      val client = FakeCardanoWalletApiClient
        .Success[IO](
          expectedPath,
          "",
          readResource("getWallet.json"),
          Some(Header("RoutingHeaderName", "RoutingHeaderValue"))
        )

      val result = client.getWallet(walletId).unsafeToFuture().futureValue
      result.isRight mustBe true

      val Right(data) = result
      data.balance.available mustBe BigInt(42000000)
      data.state.status mustBe WalletStatus.Ready
    }

    "fail on server error" in {
      val client =
        FakeCardanoWalletApiClient.Fail[IO](
          expectedPath,
          "",
          "not_found",
          "Bad request"
        )

      val error =
        client.getWallet(walletId).unsafeToFuture().futureValue.left.value

      error must be(
        ErrorResponse(
          expectedPath,
          CardanoWalletError("not_found", "Bad request")
        )
      )
    }

    "read not_responding status" in {
      val response = """{
                       |  "balance": {
                       |    "available": {
                       |      "quantity": 42000000,
                       |      "unit": "lovelace"
                       |    }
                       |  },
                       |  "state": {
                       |    "status": "not_responding"
                       |  }
                       |}""".stripMargin

      val client =
        FakeCardanoWalletApiClient.Success[IO](expectedPath, "", response)

      val result = client.getWallet(walletId).unsafeToFuture().futureValue

      result.isRight mustBe true

      val Right(data) = result

      data.state.status mustBe WalletStatus.NotResponding
    }
  }

  private def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(s"cardano/wallet/$resource").mkString
    } catch {
      case _: Throwable =>
        throw new RuntimeException(s"Resource $resource not found")
    }
  }
}

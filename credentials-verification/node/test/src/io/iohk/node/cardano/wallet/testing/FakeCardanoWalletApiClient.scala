package io.iohk.node.cardano.wallet.testing

import com.softwaremill.sttp.StringBody
import com.softwaremill.sttp.testing.SttpBackendStub
import io.circe.parser.parse
import io.iohk.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.node.cardano.wallet.api.ApiClient
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.ExecutionContext

object FakeCardanoWalletApiClient {
  object Success {
    def apply(
        expectedPath: String,
        expectedJsonRequest: String,
        responseBody: String
    )(implicit
        ec: ExecutionContext
    ): CardanoWalletApiClient = {
      FakeCardanoWalletApiClient(expectedPath, expectedJsonRequest, 200, responseBody)
    }
  }

  object Fail {
    def apply(
        expectedPath: String,
        expectedJsonRequest: String,
        errorCode: String,
        errorMessage: String
    )(implicit
        ec: ExecutionContext
    ): CardanoWalletApiClient = {
      FakeCardanoWalletApiClient(
        expectedPath,
        expectedJsonRequest,
        400,
        createJsonErrorResponse(errorCode, errorMessage)
      )
    }
  }

  private def apply(
      expectedPath: String,
      expectedJsonRequest: String,
      responseCode: Int,
      responseBody: String
  )(implicit
      ec: ExecutionContext
  ): CardanoWalletApiClient = {
    val config = ApiClient.Config("localhost", 8090)
    val backend = SttpBackendStub.asynchronousFuture
      .whenRequestMatches(request =>
        request.uri.host == config.host && request.uri.port.value == config.port && request.uri.path
          .mkString("/") == expectedPath && sameJson(request.body.asInstanceOf[StringBody].s, expectedJsonRequest)
      )
      .thenRespondWithCode(responseCode, responseBody)

    new ApiClient(config)(backend, ec)
  }

  private def sameJson(a: String, b: String): Boolean = {
    parse(a).right.value == parse(b).right.value
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

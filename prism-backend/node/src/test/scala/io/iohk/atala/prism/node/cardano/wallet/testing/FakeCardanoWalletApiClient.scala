package io.iohk.atala.prism.node.cardano.wallet.testing

import com.softwaremill.sttp.StringBody
import com.softwaremill.sttp.testing.SttpBackendStub
import io.circe.parser.parse
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.cardano.wallet.api.ApiClient
import org.scalatest.OptionValues._

import scala.concurrent.ExecutionContext

object FakeCardanoWalletApiClient {

  /** Sets up a CardanoWalletApiClient instance that will return a successful response for the given path and request.
    */
  object Success {
    def apply(
        expectedPath: String,
        expectedJsonRequest: String,
        responseBody: String,
        customHeaders: Map[String, String] = Map.empty
    )(implicit
        ec: ExecutionContext
    ): CardanoWalletApiClient = {
      FakeCardanoWalletApiClient(expectedPath, expectedJsonRequest, 200, responseBody, customHeaders)
    }
  }

  /** Sets up a CardanoWalletApiClient instance that will return a fail response for the given path and request.
    */
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

  /** Sets up a CardanoWalletApiClient instance that will return {@code 404 Not Found} errors for all requests.
    */
  object NotFound {
    def apply()(implicit
        ec: ExecutionContext
    ): CardanoWalletApiClient = {
      val config = ApiClient.Config("localhost", 8090, Map.empty)
      val backend = SttpBackendStub.asynchronousFuture

      new ApiClient(config)(backend, ec)
    }
  }

  private def apply(
      expectedPath: String,
      expectedJsonRequest: String,
      responseCode: Int,
      responseBody: String,
      customHeaders: Map[String, String] = Map.empty
  )(implicit
      ec: ExecutionContext
  ): CardanoWalletApiClient = {
    val config = ApiClient.Config("localhost", 8090, customHeaders)
    val backend = SttpBackendStub.asynchronousFuture
      .whenRequestMatches(request =>
        request.uri.host == config.host && request.uri.port.value == config.port && request.uri.path
          .mkString("/") == expectedPath && sameJson(request.body.asInstanceOf[StringBody].s, expectedJsonRequest) &&
          customHeaders.forall(header => request.headers.contains(header))
      )
      .thenRespondWithCode(responseCode, responseBody)

    new ApiClient(config)(backend, ec)
  }

  private def sameJson(a: String, b: String): Boolean = {
    a == b || parse(a).toOption.get == parse(b).toOption.get
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

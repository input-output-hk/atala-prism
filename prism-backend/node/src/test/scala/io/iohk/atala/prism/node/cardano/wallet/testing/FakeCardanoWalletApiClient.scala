package io.iohk.atala.prism.node.cardano.wallet.testing

import cats.effect.Async
import sttp.client3._
import io.circe.parser.parse
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.cardano.wallet.api.ApiClient
import org.scalatest.OptionValues._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.model.{Header, StatusCode}

object FakeCardanoWalletApiClient {

  /** Sets up a CardanoWalletApiClient instance that will return a successful response for the given path and request.
    */
  object Success {
    def apply[F[_]: Async](
        expectedPath: String,
        expectedJsonRequest: String,
        responseBody: String,
        routingHeader: Option[Header] = Option.empty
    ): CardanoWalletApiClient[F] = {
      FakeCardanoWalletApiClient(
        expectedPath,
        expectedJsonRequest,
        200,
        responseBody,
        routingHeader
      )
    }
  }

  /** Sets up a CardanoWalletApiClient instance that will return a fail response for the given path and request.
    */
  object Fail {
    def apply[F[_]: Async](
        expectedPath: String,
        expectedJsonRequest: String,
        errorCode: String,
        errorMessage: String
    ): CardanoWalletApiClient[F] = {
      FakeCardanoWalletApiClient(
        expectedPath,
        expectedJsonRequest,
        400,
        createJsonErrorResponse(errorCode, errorMessage),
        Option.empty
      )
    }
  }

  /** Sets up a CardanoWalletApiClient instance that will return {@code 404 Not Found} errors for all requests.
    */
  object NotFound {
    def apply[F[_]: Async](): CardanoWalletApiClient[F] = {
      val config = ApiClient.Config("localhost", 8090, Option.empty)
      val backend = AsyncHttpClientCatsBackend.stub
      new ApiClient(config, backend)
    }
  }

  private def apply[F[_]: Async](
      expectedPath: String,
      expectedJsonRequest: String,
      responseCode: Int,
      responseBody: String,
      maybeRoutingHeader: Option[Header]
  ): CardanoWalletApiClient[F] = {
    val config = ApiClient.Config("localhost", 8090, maybeRoutingHeader)
    val backend = AsyncHttpClientCatsBackend.stub
      .whenRequestMatches(request =>
        request.uri.host.contains(config.host)
          && request.uri.port.value == config.port
          && maybeRoutingHeader.forall(expectedRoutingHeader => request.headers.contains(expectedRoutingHeader))
          && request.uri.path
            .mkString("/") == expectedPath
          && sameJson(
            request.body.asInstanceOf[StringBody].s,
            expectedJsonRequest
          )
      )
      .thenRespondWithCode(StatusCode.apply(responseCode), responseBody)

    new ApiClient(config, backend)
  }

  private def sameJson(a: String, b: String): Boolean = {
    a == b || parse(a).toOption.get == parse(b).toOption.get
  }

  private def createJsonErrorResponse(
      errorCode: String,
      message: String
  ): String = {
    s"""
       |{
       |  "code": "$errorCode",
       |  "message": "$message"
       |}
     """.stripMargin
  }
}

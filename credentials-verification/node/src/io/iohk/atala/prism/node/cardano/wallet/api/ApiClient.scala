package io.iohk.atala.prism.node.cardano.wallet.api

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.circe.parser.parse
import io.circe.{Decoder, Json}
import io.iohk.atala.prism.models.TransactionId
import io.iohk.atala.prism.node.cardano.models.{Payment, WalletId}
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient.{CardanoWalletError, ErrorResponse, Result}
import io.iohk.atala.prism.node.cardano.wallet.api.ApiClient._
import io.iohk.atala.prism.node.cardano.wallet.api.ApiRequest.PostTransaction
import io.iohk.atala.prism.node.cardano.wallet.api.JsonCodecs._
import io.iohk.atala.prism.utils.FutureEither._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Implementation of the `CardanoWalletApiClient` that accesses the REST API provided by `cardano-wallet`.
  */
private[wallet] class ApiClient(config: ApiClient.Config)(implicit
    backend: SttpBackend[Future, Nothing],
    ec: ExecutionContext
) extends CardanoWalletApiClient { self =>

  override def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      passphrase: String
  ): Result[TransactionId] =
    PostTransaction(walletId, payments, passphrase).run(transactionIdFromTransactionDecoder)

  private def call[A: Decoder](method: ApiRequest): Result[A] = {
    sttp
      .contentType(MediaTypes.Json)
      .response(asString)
      .method(method.httpMethod, Uri.apply(config.host, config.port).path(method.path))
      .body(method.requestBody.noSpaces)
      .send()
      .map { response =>
        getResult[A](response).left
          .map(e => ErrorResponse(method.path, e))
      }
      .toFutureEither
  }

  private implicit class ApiRequestExtensions(m: ApiRequest) {
    def run[A: Decoder]: Result[A] = call[A](m)
  }
}

private[wallet] object ApiClient {

  case class Config(host: String, port: Int)

  private[wallet] val DefaultBackend: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()

  /**
    * Try to map a response to a result or an error.
    *
    * <p>If the mapping is not possible, throw an exception.
    *
    * @tparam A the success type.
    * @return The success or the error response.
    */
  private def getResult[A](response: Response[String])(implicit
      decoder: Decoder[A]
  ): Either[CardanoWalletError, A] = {
    response.body.fold(
      errorResponse => Left(unsafeToError(errorResponse)),
      successResponse => Right(unsafeToResult[A](successResponse))
    )
  }

  /**
    * Try to map a string response a Json.
    */
  private def unsafeToJson(response: String): Json = {
    parse(response).fold(
      parsingFailure =>
        throw new RuntimeException(
          s"Cardano Wallet API Error: ${parsingFailure.message}, with response: ${response.take(256)}",
          parsingFailure.underlying
        ),
      identity
    )
  }

  /**
    * Try to map a string response to its error result.
    */
  private def unsafeToError(response: String): CardanoWalletError = {
    val json = unsafeToJson(response)
    json
      .as[CardanoWalletError]
      .fold(
        decodingFailure =>
          throw new RuntimeException(s"Cardano Wallet API Error: ${decodingFailure.toString}", decodingFailure),
        identity
      )
  }

  /**
    * Try to map a string response to its success result.
    */
  private def unsafeToResult[A](response: String)(implicit decoder: Decoder[A]): A = {
    val json = unsafeToJson(response)
    json
      .as[A]
      .fold(
        decodingFailure =>
          throw new RuntimeException(s"Cardano Wallet API Error: ${decodingFailure.toString}", decodingFailure),
        identity
      )
  }
}

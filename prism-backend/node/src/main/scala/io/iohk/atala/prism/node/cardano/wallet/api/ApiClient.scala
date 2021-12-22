package io.iohk.atala.prism.node.cardano.wallet.api

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.circe.parser.parse
import io.circe.{Decoder, Json}
import io.iohk.atala.prism.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.node.cardano.models.{Payment, TransactionMetadata, WalletId}
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient.{
  CardanoWalletError,
  ErrorResponse,
  EstimatedFee,
  Result
}
import io.iohk.atala.prism.node.cardano.wallet.api.ApiClient._
import io.iohk.atala.prism.node.cardano.wallet.api.ApiRequest.{
  DeleteTransaction,
  EstimateTransactionFee,
  GetTransaction,
  GetWallet,
  PostTransaction
}
import io.iohk.atala.prism.node.cardano.wallet.api.JsonCodecs._
import io.iohk.atala.prism.node.models.WalletDetails
import io.iohk.atala.prism.utils.FutureEither._

import scala.concurrent.{ExecutionContext, Future}

/** Implementation of the `CardanoWalletApiClient` that accesses the REST API provided by `cardano-wallet`.
  */
private[wallet] class ApiClient(config: ApiClient.Config)(implicit
    backend: SttpBackend[Future, Nothing],
    ec: ExecutionContext
) extends CardanoWalletApiClient { self =>
  override def estimateTransactionFee(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata]
  ): Result[EstimatedFee] = {
    EstimateTransactionFee(walletId, payments, metadata).run
  }

  override def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ): Result[TransactionId] = {
    PostTransaction(walletId, payments, metadata, passphrase).run(transactionIdFromTransactionDecoder)
  }

  override def getTransaction(walletId: WalletId, transactionId: TransactionId): Result[TransactionDetails] = {
    GetTransaction(walletId, transactionId).run
  }

  override def deleteTransaction(walletId: WalletId, transactionId: TransactionId): Result[Unit] = {
    DeleteTransaction(walletId, transactionId).run
  }

  override def getWallet(walletId: WalletId): Result[WalletDetails] = {
    GetWallet(walletId).run
  }

  private def call[A: Decoder](method: ApiRequest): Result[A] = {
    sttp
      .contentType(MediaTypes.Json)
      .response(asString)
      .headers(config.customHeaders)
      .method(method.httpMethod, Uri.apply(config.host, config.port).path(method.path))
      .body(method.requestBody.map(_.noSpaces).getOrElse(""))
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

  case class Config(host: String, port: Int, customHeaders: Map[String, String])

  private[wallet] val DefaultBackend: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()

  /** Try to map a response to a result or an error.
    *
    * <p>If the mapping is not possible, throw an exception.
    *
    * @tparam A
    *   the success type.
    * @return
    *   The success or the error response.
    */
  private def getResult[A](response: Response[String])(implicit
      decoder: Decoder[A]
  ): Either[CardanoWalletError, A] = {
    response.body.fold(
      errorResponse => Left(unsafeToError(errorResponse)),
      successResponse => Right(unsafeToResult[A](successResponse))
    )
  }

  /** Try to map a string response a Json.
    */
  private def unsafeToJson(response: String): Json = {
    if (response.isEmpty) {
      // Use an empty JSON to represent empty response to simplify decoding
      Json.obj()
    } else {
      parse(response).fold(
        parsingFailure =>
          throw new RuntimeException(
            s"Cardano Wallet API Error: ${parsingFailure.message}, with response: ${response.take(256)}",
            parsingFailure.underlying
          ),
        identity
      )
    }
  }

  /** Try to map a string response to its error result.
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

  /** Try to map a string response to its success result.
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

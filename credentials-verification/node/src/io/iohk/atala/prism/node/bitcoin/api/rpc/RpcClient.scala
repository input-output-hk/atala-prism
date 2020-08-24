package io.iohk.atala.prism.node.bitcoin
package api
package rpc

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.circe.parser.parse
import io.circe.{Decoder, Json}
import io.iohk.atala.prism.node.bitcoin.models._
import io.iohk.cvp.utils.FutureEither._

import scala.concurrent.{ExecutionContext, Future}
import JsonCodecs._
import ApiModel._
import RpcMethod._
import BitcoinApiClient._
import RpcClient._

/**
  * Implementation of the `BitcoinApiClient` that accesses the RPC provided by bitcoind
  */
class RpcClient(config: RpcClient.Config)(implicit
    backend: SttpBackend[Future, Nothing],
    ec: ExecutionContext
) extends BitcoinApiClient { self =>

  override def getBlock(blockhash: Blockhash, verbosity: BlockVerbosity): Result[Block] =
    verbosity match {
      case BlockVerbosity.Raw =>
        GetBlock(blockhash, verbosity).run[Block.Canonical]
      case BlockVerbosity.Full =>
        GetBlock(blockhash, verbosity).run[Block.Full]
    }

  override def getBlockhash(height: Int): Result[Blockhash] =
    GetBlockhash(height).run

  override def getBestBlockhash(): Result[Blockhash] =
    GetBestBlockhash.run

  override def listUnspent(
      minconf: Int = 1,
      maxconf: Int = 9999999,
      addresses: List[Address] = Nil
  ): Result[List[Utxo]] =
    ListUnspent(minconf, maxconf, addresses).run

  override def getRawChangeAddress(addressType: Option[AddressType]): Result[Address] =
    GetRawChangeAddress(addressType).run

  override def createRawTransaction(
      inputs: List[TransactionInput],
      outputs: TransactionOutput,
      locktime: Long = 0L,
      replaceable: Boolean = false
  ): Result[RawTransaction] =
    CreateRawTransaction(inputs, outputs, locktime, replaceable).run

  override def signRawTransactionWithWallet(
      rawTransaction: RawTransaction
  ): Result[SignRawTransactionWithWalletResult] =
    SignRawTransactionWithWallet(rawTransaction).run

  override def sendRawTransaction(tx: RawSignedTransaction, allowHighFees: Boolean = false): Result[TransactionId] =
    SendRawTransaction(tx, allowHighFees).run

  private val server = sttp
    .post(Uri.apply(config.host, config.port))
    .header("Content-Type", "text/plain")
    .response(asString)
    .auth
    .basic(config.username, config.password)

  private def call[A: Decoder](method: RpcMethod): Result[A] = {
    server
      .body(method.toJsonString)
      .send()
      .map { response =>
        getResult[A](response).left
          .map(e => ErrorResponse(method.name, e))
      }
      .toFutureEither
  }

  private implicit class RpcMethodExtensions(m: RpcMethod) {
    def run[A: Decoder]: Result[A] = call[A](m)
  }
}

object RpcClient {

  case class Config(host: String, port: Int, username: String, password: String)

  private[api] val DefaultBackend: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()

  /**
    * Try to map a response to a result or an error.
    *
    * If the mapping is not possible, throw an exception.
    *
    * @tparam A the success type.
    * @return The success or the error response.
    */
  private def getResult[A](response: Response[String])(implicit
      decoder: Decoder[A]
  ): Either[BitcoinError, A] = {

    response.body.fold(
      fa = errorResponse => Left(unsafeToError(errorResponse)),
      fb = successResponse => Right(unsafeToResult[A](successResponse))
    )
  }

  /**
    * Try to map a string response a Json.
    */
  private def unsafeToJson(response: String): Json = {
    parse(response).fold(
      fa = parsingFailure =>
        // It is likely that the response is just an string, which can happen when the server is overloaded
        throw new RuntimeException(s"Bitcoin API Error: ${parsingFailure.message}", parsingFailure.underlying),
      fb = result => result
    )
  }

  /**
    * Try to map a string response to its error result.
    */
  private def unsafeToError(response: String): BitcoinError = {
    val json = unsafeToJson(response)
    json.hcursor
      .downField("error")
      .as[BitcoinError]
      .fold(
        fa = decodingFailure => throw new RuntimeException(s"Bitcoin API Error: ${decodingFailure.message}"),
        fb = identity
      )
  }

  /**
    * Try to map a string response to its success result.
    */
  private def unsafeToResult[A](response: String)(implicit decoder: Decoder[A]): A = {
    val json = unsafeToJson(response)
    json.hcursor
      .downField("result")
      .as[A]
      .fold(
        fa = decodingFailure => throw new RuntimeException(s"Bitcoin API Error: ${decodingFailure.message}"),
        fb = result => result
      )
  }

}

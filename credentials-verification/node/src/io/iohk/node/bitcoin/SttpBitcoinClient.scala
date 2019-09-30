package io.iohk.node.bitcoin

import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import com.softwaremill.sttp.{Response, SttpBackend, Uri, asString, sttp}
import io.circe.parser.parse
import io.circe.{Decoder, Json}
import io.iohk.node.bitcoin.models.{Block, BlockError, Blockhash}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SttpBitcoinClient(config: BitcoinClient.Config)(implicit ec: ExecutionContext) extends BitcoinClient {

  import BitcoinClient._
  import SttpBitcoinClient._

  private implicit val backend: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()

  private val server = sttp
    .post(Uri.apply(config.host, config.port))
    .header("Content-Type", "text/plain")
    .response(asString)
    .auth
    .basic(config.username, config.password)

  override def getBlock(blockhash: Blockhash): Result[BlockError.NotFound, Block] = {
    val errorCodeMapper = Map(-5 -> BlockError.NotFound(blockhash))
    val method = BitcoinRPCMethod.GetBlock(blockhash)
    call[BlockError.NotFound, Block](method, errorCodeMapper)
  }

  override def getLatestBlockhash: Result[Nothing, Blockhash] = {
    val method = BitcoinRPCMethod.GetBestBlockhash
    call[Blockhash](method)
  }

  override def getBlockhash(height: Int): Result[BlockError.HeightNotFound, Blockhash] = {
    val errorCodeMapper = Map(-8 -> BlockError.HeightNotFound(height))
    val method = BitcoinRPCMethod.GetBlockhash(height)
    call[BlockError.HeightNotFound, Blockhash](method, errorCodeMapper)
  }

  private def call[A: Decoder](method: BitcoinRPCMethod): Result[Nothing, A] = {
    call[Nothing, A](method, Map.empty[Int, Nothing])
  }

  private def call[E, A: Decoder](method: BitcoinRPCMethod, errorCodeMapper: Map[Int, E] = Map.empty): Result[E, A] = {

    server.body(method.toJsonString).send().map { response =>
      getResult[E, A](response, errorCodeMapper)
    }
  }
}

object SttpBitcoinClient {

  private final case class RPCErrorResponse(code: Int, message: String)

  private implicit val rpcErrorResponseDecoder: Decoder[RPCErrorResponse] =
    Decoder.forProduct2("code", "message")(RPCErrorResponse.apply)

  /**
    * Try to map a response to a result or an error.
    *
    * If the mapping is not possible, throw an exception.
    *
    * @tparam E the error type.
    * @tparam A the success type.
    * @return The success or the error response.
    */
  private def getResult[E, A](response: Response[String], errorCodeMapper: Map[Int, E] = Map.empty)(
      implicit decoder: Decoder[A]
  ): Either[E, A] = {

    response.body.fold(
      fa = errorResponse => Left(unsafeToError(errorResponse, errorCodeMapper)),
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
  private def unsafeToError[E](response: String, errorCodeMapper: Map[Int, E]): E = {
    val json = unsafeToJson(response)
    json.hcursor
      .downField("error")
      .as[RPCErrorResponse]
      .fold(
        fa = decodingFailure => throw new RuntimeException(s"Bitcoin API Error: ${decodingFailure.message}"),
        fb = {
          case RPCErrorResponse(code, _) if errorCodeMapper.contains(code) =>
            errorCodeMapper(code)
          case RPCErrorResponse(code, message) =>
            throw new RuntimeException(s"Bitcoin API Error $code: $message")
        }
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

  private implicit val blockhashDecoder: Decoder[Blockhash] = implicitly[Decoder[String]].emapTry { string =>
    Try(Blockhash.from(string).getOrElse(throw new RuntimeException("Invalid blockhash")))
  }

  private implicit val blockDecoder: Decoder[Block] =
    Decoder.forProduct4("hash", "height", "time", "previousblockhash")(Block.apply)

}

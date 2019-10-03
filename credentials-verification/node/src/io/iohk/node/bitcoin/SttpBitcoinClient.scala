package io.iohk.node.bitcoin

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.circe.parser.parse
import io.circe.{Decoder, Json}
import io.iohk.node.bitcoin.models._
import io.iohk.node.utils.FutureEither._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SttpBitcoinClient(config: BitcoinClient.Config)(
    implicit backend: SttpBackend[Future, Nothing],
    ec: ExecutionContext
) extends BitcoinClient {

  import BitcoinClient._
  import SttpBitcoinClient._

  private val server = sttp
    .post(Uri.apply(config.host, config.port))
    .header("Content-Type", "text/plain")
    .response(asString)
    .auth
    .basic(config.username, config.password)

  override def getBlock(blockhash: Blockhash): Result[BlockError.NotFound, Block.Canonical] = {
    val errorCodeMapper = Map(-5 -> BlockError.NotFound(blockhash))
    val method = BitcoinRPCMethod.GetBlock(blockhash, BitcoinRPCMethod.BlockVerbosity.Raw)
    call[BlockError.NotFound, Block.Canonical](method, errorCodeMapper)
  }

  override def getFullBlock(blockhash: Blockhash): Result[BlockError.NotFound, Block.Full] = {
    val errorCodeMapper = Map(-5 -> BlockError.NotFound(blockhash))
    val method = BitcoinRPCMethod.GetBlock(blockhash, BitcoinRPCMethod.BlockVerbosity.Full)
    call[BlockError.NotFound, Block.Full](method, errorCodeMapper)
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
    server
      .body(method.toJsonString)
      .send()
      .map { response =>
        getResult[E, A](response, errorCodeMapper)
      }
      .toFutureEither
  }
}

object SttpBitcoinClient {

  private final case class RPCErrorResponse(code: Int, message: String)

  private val DefaultBackend: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()

  def apply(config: BitcoinClient.Config)(implicit ec: ExecutionContext): SttpBitcoinClient = {
    new SttpBitcoinClient(config)(DefaultBackend, ec)
  }

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

  private implicit val blockhashDecoder: Decoder[Blockhash] = Decoder.decodeString.emapTry { string =>
    Try(Blockhash.from(string).getOrElse(throw new RuntimeException("Invalid blockhash")))
  }

  private implicit val blockHeaderDecoder: Decoder[BlockHeader] =
    Decoder.forProduct4("hash", "height", "time", "previousblockhash")(BlockHeader.apply)

  private implicit val canonicalBlockDecoder: Decoder[Block.Canonical] = blockHeaderDecoder.map(Block.Canonical.apply)

  private implicit val transactionIdDecoder: Decoder[TransactionId] = Decoder.decodeString.emapTry { string =>
    Try(TransactionId.from(string).getOrElse(throw new RuntimeException("Invalid transaction id")))
  }

  private implicit val transactionOutputScriptDecoder: Decoder[Transaction.OutputScript] =
    Decoder.forProduct2("type", "asm")(Transaction.OutputScript.apply)

  private implicit val transactionOutputDecoder: Decoder[Transaction.Output] =
    Decoder.forProduct3("value", "n", "scriptPubKey")(Transaction.Output.apply)

  private implicit val fullBlockDecoder: Decoder[Block.Full] = {
    Decoder.decodeJson.emapTry { json =>
      val result = for {
        header <- json.as[BlockHeader]
        txDecoder = transactionDecoder(header.hash)
        txs <- json.hcursor.downField("tx").as[List[Transaction]](Decoder.decodeList(txDecoder))
      } yield Block.Full(header, txs)

      result match {
        case Left(e) => Failure(e)
        case Right(x) => Success(x)
      }
    }
  }

  private def transactionDecoder(blockhash: Blockhash): Decoder[Transaction] = {
    Decoder.forProduct2[Transaction, TransactionId, List[Transaction.Output]]("txid", "vout")(
      (id, vout) => Transaction(id = id, vout = vout, blockhash = blockhash)
    )
  }
}

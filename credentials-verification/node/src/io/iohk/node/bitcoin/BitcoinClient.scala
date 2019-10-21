package io.iohk.node.bitcoin

import io.iohk.node.bitcoin.models._
import io.iohk.cvp.utils.FutureEither

import scala.concurrent.ExecutionContext
import api.BitcoinApiClient
import api.ApiModel._
import BitcoinClient.Result
import BitcoinClient.ApiResultExtensions

/**
  * Implementation of the operations we need to perform against the Bitcoin network
  */
class BitcoinClient(apiClient: BitcoinApiClient)(implicit ec: ExecutionContext) {

  def getBlock(blockhash: Blockhash): Result[BlockError.NotFound, Block.Canonical] =
    apiClient
      .getBlock(blockhash, BlockVerbosity.Raw)
      .withErrors {
        case -5 => BlockError.NotFound(blockhash)
      }
      .innerFlatMap {
        case b: Block.Canonical =>
          Right(b)
        case _: Block.Full =>
          throw new RuntimeException(s"Bitcoin API Error: full block retrieved when raw one expected")
      }

  def getFullBlock(blockhash: Blockhash): Result[BlockError.NotFound, Block.Full] =
    apiClient
      .getBlock(blockhash, BlockVerbosity.Full)
      .withErrors {
        case -5 => BlockError.NotFound(blockhash)
      }
      .innerFlatMap {
        case _: Block.Canonical =>
          throw new RuntimeException(s"Bitcoin API Error: missing list of transactions of full block")
        case b: Block.Full =>
          Right(b)
      }

  // Bitcoin always has at least 1 block (the genesis one)
  def getLatestBlockhash: Result[Nothing, Blockhash] =
    apiClient.getBestBlockhash().withNoErrors

  def getBlockhash(height: Int): Result[BlockError.HeightNotFound, Blockhash] =
    apiClient
      .getBlockhash(height)
      .withErrors {
        case -8 => BlockError.HeightNotFound(height)
      }

  def sendDataTx(opReturnData: OpData): Result[SendDataTxError, TransactionId] = {
    val BITCOIN_FEE = 0.0005
    val INTERNAL_ERROR = "opReturnTx"

    // TODO: We need to understand who to correctly represent the OP_RETURN data in a transaction
    def encodeOpReturnData(in: OpData): String = {
      val hex = in.toHex
      if (hex.length > OP_RETURN_MAX_LENGTH) {
        // This should be impossible, the OpData type should be only instantiatable if
        // its hex representation is less than or equal to OP_RETURN_MAX_LENGTH in length
        throw new RuntimeException("FATAL Representation Error: the hex representation of the OpReturn is too long")
      } else if (hex.length == OP_RETURN_MAX_LENGTH) hex
      else {
        val n = OP_RETURN_MAX_LENGTH - hex.length
        ("0" * n) ++ hex
      }
    }

    def composeTx(unspent: Utxo, changeAddress: Address): (TransactionInput, TransactionOutput) = {
      val ti: TransactionInput = TransactionInput(unspent.txid, unspent.vout, None)
      val to: TransactionOutput = TransactionOutput(
        Some(encodeOpReturnData(opReturnData)),
        Map(changeAddress -> Btc(unspent.amount - BITCOIN_FEE))
      )
      (ti, to)
    }

    val comp = for {
      unspent <- apiClient
        .listUnspent()
        .map(_.filter(_.amount >= BITCOIN_FEE)) // TODO: accumulate UTXOs until we get enough to pay the fee
        .failWhen(_.isEmpty)(INTERNAL_ERROR, 1)
        .map(_.head)
      changeAddress <- apiClient.getRawChangeAddress()
      (ti, to) = composeTx(unspent, changeAddress)
      rawTx <- apiClient.createRawTransaction(List(ti), to)
      signRawTx <- apiClient.signRawTransactionWithWallet(rawTx)
      txid <- apiClient.sendRawTransaction(signRawTx.hex)
    } yield {
      txid
    }

    // Using the following link as a reference, I'm going to tackle some of the general case errors:
    // https://github.com/bitcoin/bitcoin/blob/v0.18.1/src/rpc/protocol.h#L34-L92
    // Even so, given the lack of documentation, beyond this general errors, I'm going to suppose that
    // `listUnspent`, `signRawTransactionWithWallet` and `createRawTransaction` don't fail. We will
    // need to fill this things up iteratively when we improve our knowledge about this
    // Also, I believe that `getRawChangeAddress` shouldn't fail
    comp.withErrors {
      case (INTERNAL_ERROR, 1) => SendDataTxError.CanNotPayTxFee
      case -3 | -32602 | -5 => SendDataTxError.InvalidRequest
      case -26 | -22 => SendDataTxError.InvalidTransaction
      case -27 => SendDataTxError.RawTransactionAlreadyExists
    }
  }
}

object BitcoinClient {

  type Result[E, A] = FutureEither[E, A]
  type Config = api.BitcoinApiClient.Config
  val Config = api.BitcoinApiClient.Config

  def apply(config: Config)(implicit ec: ExecutionContext): BitcoinClient = {
    val client = api.BitcoinApiClient(config)
    new BitcoinClient(client)
  }

  private[bitcoin] implicit class ApiResultExtensions[A](lr: BitcoinApiClient.Result[A]) {
    import BitcoinApiClient.{BitcoinError, ErrorResponse}

    def withNoErrors(implicit ec: ExecutionContext): Result[Nothing, A] =
      withErrors[Nothing](PartialFunction.empty)

    def withErrors[E](f: PartialFunction[Any, E])(implicit ec: ExecutionContext): Result[E, A] =
      lr.mapLeft {
        case ErrorResponse(name, BitcoinError(code, message)) if f.isDefinedAt((name, code)) =>
          f((name, code))
        case ErrorResponse(_, BitcoinError(code, message)) if f.isDefinedAt(code) =>
          f(code)
        case ErrorResponse(name, BitcoinError(code, message)) =>
          throw new RuntimeException(s"Bitcoin API Error invoking $name: [$code] $message")
      }

    def failWhen(
        p: A => Boolean
    )(name: String, code: Int, message: String = "")(implicit ec: ExecutionContext): BitcoinApiClient.Result[A] =
      lr.innerFlatMap { a: A =>
        if (p(a)) Left(ErrorResponse(name, BitcoinError(code, message))) else Right(a)
      }

  }
}

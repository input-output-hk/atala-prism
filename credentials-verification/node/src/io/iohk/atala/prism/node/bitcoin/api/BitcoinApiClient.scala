package io.iohk.atala.prism.node.bitcoin.api

import io.iohk.atala.prism.node.bitcoin.models._
import io.iohk.atala.prism.utils.FutureEither

import scala.concurrent.ExecutionContext
import ApiModel._
import io.iohk.atala.prism.node.models.TransactionId

/**
  * Client for the public Bitcoin API. Trimmed down to the methods we need
  */
trait BitcoinApiClient {

  import BitcoinApiClient._

  def getBlock(blockhash: Blockhash, verbosity: BlockVerbosity): Result[Block]
  def getBlockhash(height: Int): Result[Blockhash]
  def getBestBlockhash(): Result[Blockhash]

  def listUnspent(minconf: Int = 1, maxconf: Int = 9999999, addresses: List[Address] = Nil): Result[List[Utxo]]
  def getRawChangeAddress(addressType: Option[AddressType]): Result[Address]
  def createRawTransaction(
      inputs: List[TransactionInput],
      outputs: TransactionOutput,
      locktime: Long = 0L,
      replaceable: Boolean = false
  ): Result[RawTransaction]
  def signRawTransactionWithWallet(rawTransaction: RawTransaction): Result[SignRawTransactionWithWalletResult]
  def sendRawTransaction(tx: RawSignedTransaction, allowHighFees: Boolean = false): Result[TransactionId]

  final def getRawChangeAddress(addressType: AddressType): Result[Address] =
    getRawChangeAddress(Some(addressType))
  final def getRawChangeAddress(): Result[Address] =
    getRawChangeAddress(None)
}

object BitcoinApiClient {

  type Config = rpc.RpcClient.Config
  val Config = rpc.RpcClient.Config.apply _

  def apply(config: Config)(implicit ec: ExecutionContext): BitcoinApiClient = {
    new rpc.RpcClient(config)(rpc.RpcClient.DefaultBackend, ec)
  }

  type Result[A] = FutureEither[ErrorResponse, A]

  final case class BitcoinError(code: Int, message: String)
  final case class ErrorResponse(rpcMethod: String, error: BitcoinError)
}

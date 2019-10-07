package io.iohk.node.bitcoin
package api
package rpc

import io.iohk.node.bitcoin.models._
import io.circe.{Json, Encoder}
import io.circe.syntax._
import JsonCodecs._
import scala.language.implicitConversions

private[rpc] sealed abstract class RpcMethod(val name: String) extends Product with Serializable {
  def toJsonString: String = {
    s"""{ "jsonrpc": "1.0", "method": "$name", "params": $arrayParams }"""
  }

  protected def arrayParams: String = "[]"
}

private[rpc] sealed abstract class CirceRpcMethod(name: String) extends RpcMethod(name) {
  protected def parameters: List[Json]
  override final def arrayParams: String = Json.fromValues(parameters).noSpaces
  protected final def args(js: Json*): List[Json] = js.toList
  protected final implicit def jsonView[T](t: T)(implicit e: Encoder[T]): Json =
    t.asJson
}

object RpcMethod {

  import ApiModel._

  final case class GetBlock(blockhash: Blockhash, verbosity: BlockVerbosity) extends CirceRpcMethod("getblock") {
    override def parameters = args(blockhash, verbosity)
  }

  final case class GetBlockhash(height: Int) extends CirceRpcMethod("getblockhash") {
    override def parameters = args(height)
  }

  final case object GetBestBlockhash extends RpcMethod("getbestblockhash")

  final case class ListUnspent(minconf: Int = 1, maxconf: Int = 9999999, addresses: List[Address] = Nil)
      extends CirceRpcMethod("listunspent") {
    override def parameters = args(minconf, maxconf, addresses)
  }

  final case class GetRawChangeAddress(addressType: Option[AddressType] = None)
      extends CirceRpcMethod("getrawchangeaddress") {
    override def parameters =
      addressType.map(args(_)).getOrElse(Nil)
  }

  final object GetRawChangeAddress {
    def apply(addressType: AddressType): GetRawChangeAddress = new GetRawChangeAddress(Some(addressType))
  }

  final case class CreateRawTransaction(
      inputs: List[TransactionInput],
      outputs: TransactionOutput,
      locktime: Long = 0L,
      replaceable: Boolean = false
  ) extends CirceRpcMethod("createrawtransaction") {
    override def parameters = args(inputs, outputs, locktime, replaceable)
  }

  final case class SignRawTransactionWithWallet(rawTransaction: RawTransaction)
      extends CirceRpcMethod("signrawtransactionwithwallet") {
    override def parameters = args(rawTransaction)
  }

  final case class SendRawTransaction(tx: RawSignedTransaction, allowHighFees: Boolean = false)
      extends CirceRpcMethod("sendrawtransaction") {
    override def parameters = args(tx, allowHighFees)
  }
}

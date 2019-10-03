package io.iohk.node.bitcoin

import io.iohk.node.bitcoin.models.Blockhash

private[bitcoin] sealed abstract class BitcoinRPCMethod(name: String) extends Product with Serializable {
  def toJsonString: String = {
    s"""{ "jsonrpc": "1.0", "method": "$name", "params": $arrayParams }"""
  }

  protected def arrayParams: String = "[]"

  protected def asParamString(string: String): String = s""""$string""""
}

object BitcoinRPCMethod {
  sealed abstract class BlockVerbosity(val int: Int)
  object BlockVerbosity {
    final case object Raw extends BlockVerbosity(1) // just block header expanded
    final case object Full extends BlockVerbosity(2) // block header and detailed transactions
  }

  final case class GetBlock(blockhash: Blockhash, verbosity: BlockVerbosity) extends BitcoinRPCMethod("getblock") {
    override def arrayParams: String = s"[${asParamString(blockhash.string)}, ${verbosity.int}]"
  }

  final case class GetBlockhash(height: Int) extends BitcoinRPCMethod("getblockhash") {
    override protected def arrayParams: String = s"[$height]"
  }

  final case object GetBestBlockhash extends BitcoinRPCMethod("getbestblockhash")
}

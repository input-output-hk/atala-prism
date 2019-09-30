package io.iohk.node.bitcoin

private[bitcoin] sealed abstract class BitcoinRPCMethod(name: String) extends Product with Serializable {
  def toJsonString: String = {
    s"""{ "jsonrpc": "1.0", "method": "$name", "params": $arrayParams }"""
  }

  protected def arrayParams: String = "[]"

  protected def asParamString(string: String): String = s""""$string""""
}

object BitcoinRPCMethod {
  final case class GetBlock(blockhash: Blockhash) extends BitcoinRPCMethod("getblock") {
    override def arrayParams: String = s"[${asParamString(blockhash.string)}]"
  }

  final case class GetBlockhash(height: Int) extends BitcoinRPCMethod("getblockhash") {
    override protected def arrayParams: String = s"[$height]"
  }

  final case object GetBestBlockhash extends BitcoinRPCMethod("getbestblockhash")
}

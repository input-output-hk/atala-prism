package io.iohk.node.bitcoin.models

case class Transaction(id: TransactionId, blockhash: Blockhash, vout: List[Transaction.Output])

object Transaction {
  case class Output(value: BigDecimal, n: Int, scriptPubKey: OutputScript)
  case class OutputScript(`type`: String, asm: String)
}

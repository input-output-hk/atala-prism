package io.iohk.atala.prism.node.bitcoin.models

import io.iohk.atala.prism.node.models.TransactionId

case class Transaction(id: TransactionId, blockhash: Blockhash, vout: List[Transaction.Output])

object Transaction {
  case class Output(value: Btc, n: Int, scriptPubKey: OutputScript)
  case class OutputScript(`type`: String, asm: String)
}

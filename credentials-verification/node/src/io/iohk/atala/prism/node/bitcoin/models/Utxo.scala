package io.iohk.atala.prism.node.bitcoin.models

case class Utxo(txid: TransactionId, vout: Vout, amount: Btc)

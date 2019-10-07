package io.iohk.node.bitcoin.models

case class Utxo(txid: TransactionId, vout: Vout, amount: Btc)

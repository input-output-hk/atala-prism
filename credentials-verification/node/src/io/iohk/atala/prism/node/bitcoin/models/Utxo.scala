package io.iohk.atala.prism.node.bitcoin.models

import io.iohk.atala.prism.node.models.TransactionId

case class Utxo(txid: TransactionId, vout: Vout, amount: Btc)

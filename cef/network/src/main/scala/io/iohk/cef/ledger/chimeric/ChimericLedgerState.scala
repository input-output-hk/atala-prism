package io.iohk.cef.ledger.chimeric

case class ChimericLedgerState(currencies: Map[Currency, Create], accountBalance: Map[Address, Double], utxos: Set[TxOutRef])

package io.iohk.cef.ledger.chimeric

sealed trait ChimericTxFragment

class Fee(value: Value) extends ChimericTxFragment
class Mint(value: Value) extends ChimericTxFragment
class Create(currency: Currency) extends ChimericTxFragment
class Output(address: Address, value: Value) extends ChimericTxFragment
class Input(txOutRef: TxOutRef) extends ChimericTxFragment
class Deposit(address: Address, value: Value) extends ChimericTxFragment
class Withdrawal(address: Address, value: Value, nonce: Int) extends ChimericTxFragment
class LedgerId(id: Int) extends ChimericTxFragment

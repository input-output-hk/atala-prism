package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.{LedgerState, Transaction}

//Using old model and not the tx frag model
sealed trait ChimericTx extends Transaction[LedgerState[ValueHolder, Value], ValueHolder]

class UtxoTransaction(inputs: Set[Input], outputs: List[Output], forge: Value, fee: Value) extends ChimericTx

class AccountTransaction(sender: Option[Address], receiver: Option[Address], value: Value, forge: Value, fee: Value, nonce: Int) extends ChimericTx

class HybridTransaction(inputs: Map[Address, Value], outputs: Map[Address, Value], forge: Value, fee: Value, nonce: Int) extends ChimericTx

class DepositTransaction(inputs: Set[Input], depositor: Address, forge: Value, fee: Value) extends ChimericTx

class WithdrawTransaction(withdrawer: Address, outputs: List[Output], forge: Value, fee: Value, nonce: Int) extends ChimericTx




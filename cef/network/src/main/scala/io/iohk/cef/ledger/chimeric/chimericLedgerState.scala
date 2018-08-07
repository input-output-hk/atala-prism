package io.iohk.cef.ledger.chimeric

object ChimericLedgerState {
  private val AddressPrefix = "ad="
  private val TxOutRefPrefix = "to="
  private val CurrencyPrefix = "cr="

  def getAddressPartitionId(address: Address): String = s"$AddressPrefix$address"
  def getPartitionId(txOutRef: TxOutRef): String = s"$TxOutRefPrefix${txOutRef.index}--${txOutRef.id}"
  def getCurrencyPartitionId(currency: Currency): String = s"$CurrencyPrefix$currency"

  def toStateKey(partitionId: String): ChimericStateKey = partitionId.take(3) match {
    case AddressPrefix =>
      AddressHolder(partitionId.drop(3))
    case TxOutRefPrefix =>
      val utxo = partitionId.drop(3).split("--")
      UtxoHolder(TxOutRef(utxo.head, utxo.tail.head.toInt))
    case CurrencyPrefix =>
      CurrencyHolder(partitionId.drop(3))
    case _ => throw new IllegalStateException(s"Invalid partition key prefix: ${partitionId.take(3)}")
  }
}

//Values
sealed trait ChimericStateValue

case class ValueHolder(value: Value) extends ChimericStateValue
case class CreateCurrencyHolder(createCurrency: CreateCurrency) extends ChimericStateValue

//Keys
sealed trait ChimericStateKey

case class AddressHolder(address: Address) extends ChimericStateKey
case class CurrencyHolder(currency: Currency) extends ChimericStateKey
case class UtxoHolder(txOutRef: TxOutRef) extends ChimericStateKey

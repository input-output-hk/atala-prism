package io.iohk.cef.ledger.chimeric

object ChimericLedgerState {
  private val AddressPrefix = "ad="
  private val TxOutRefPrefix = "to="
  private val CurrencyPrefix = "cr="
  private val PrefixLength = 3
  private val Delimiter = "--"
  require(AddressPrefix.size == PrefixLength &&
    TxOutRefPrefix.size == PrefixLength &&
    CurrencyPrefix.size == PrefixLength)

  def getAddressPartitionId(address: Address): String = s"$AddressPrefix$address"
  def getUtxoPartitionId(txOutRef: TxOutRef): String = s"$TxOutRefPrefix${txOutRef.txId}${Delimiter}${txOutRef.index}"
  def getCurrencyPartitionId(currency: Currency): String = s"$CurrencyPrefix$currency"

  def toStateKey(partitionId: String): ChimericStateKey = partitionId.take(PrefixLength) match {
    case AddressPrefix =>
      AddressHolder(partitionId.drop(PrefixLength))
    case TxOutRefPrefix =>
      val utxo = partitionId.drop(PrefixLength).split(Delimiter)
      if (utxo.size != 2) {
        throw new IllegalArgumentException(s"Wrong format for the utxo key ${partitionId}")
      } else {
        UtxoHolder(TxOutRef(utxo.head, utxo.tail.head.toInt))
      }
    case CurrencyPrefix =>
      CurrencyHolder(partitionId.drop(PrefixLength))
    case _ => throw new IllegalStateException(s"Invalid partition key prefix: ${partitionId.take(PrefixLength)}")
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

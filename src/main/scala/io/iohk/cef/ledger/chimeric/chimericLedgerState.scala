package io.iohk.cef.ledger.chimeric

object ChimericLedgerState {

  private val AddressValuePrefix = "ad="
  private val AddressNoncePrefix = "an="
  private val TxOutRefPrefix = "to="
  private val CurrencyPrefix = "cr="
  private val PrefixLength = 3
  private val Delimiter = "--"

  List(AddressValuePrefix, AddressNoncePrefix, TxOutRefPrefix, CurrencyPrefix).foreach { prefix =>
    require(prefix.size == PrefixLength)
  }

  def getAddressValuePartitionId(address: Address): String = s"$AddressValuePrefix$address"
  def getAddressNoncePartitionId(address: Address): String = s"$AddressNoncePrefix$address"
  def getUtxoPartitionId(txOutRef: TxOutRef): String = s"$TxOutRefPrefix${txOutRef.txId}${Delimiter}${txOutRef.index}"
  def getCurrencyPartitionId(currency: Currency): String = s"$CurrencyPrefix$currency"

  def toStateKey(partitionId: String): ChimericStateKey = partitionId.take(PrefixLength) match {
    case AddressValuePrefix =>
      AddressValueKey(partitionId.drop(PrefixLength))

    case AddressNoncePrefix =>
      AddressNonceKey(partitionId.drop(PrefixLength))

    case TxOutRefPrefix =>
      val utxo = partitionId.drop(PrefixLength).split(Delimiter)
      if (utxo.size != 2) {
        throw new IllegalArgumentException(s"Wrong format for the utxo key ${partitionId}")
      } else {
        UtxoValueKey(TxOutRef(utxo.head, utxo.tail.head.toInt))
      }

    case CurrencyPrefix =>
      CurrencyKey(partitionId.drop(PrefixLength))

    case _ => throw new IllegalStateException(s"Invalid partition key prefix: ${partitionId.take(PrefixLength)}")
  }
}

// Values
sealed trait ChimericStateValue
final case class ValueHolder(value: Value) extends ChimericStateValue
final case class NonceHolder(nonce: Int) extends ChimericStateValue
final case class CreateCurrencyHolder(createCurrency: CreateCurrency) extends ChimericStateValue

// Keys
sealed trait ChimericStateKey
final case class AddressValueKey(address: Address) extends ChimericStateKey
final case class AddressNonceKey(address: Address) extends ChimericStateKey
final case class UtxoValueKey(txOutRef: TxOutRef) extends ChimericStateKey
final case class CurrencyKey(currency: Currency) extends ChimericStateKey

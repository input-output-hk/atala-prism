package io.iohk.cef.ledger.chimeric

import io.iohk.cef.crypto.SigningPublicKey

object ChimericLedgerState {

  private val AddressPrefix = "ad="
  private val AddressNoncePrefix = "an="
  private val TxOutRefPrefix = "to="
  private val CurrencyPrefix = "cr="
  private val PrefixLength = 3
  private val Delimiter = "--"

  List(AddressPrefix, AddressNoncePrefix, TxOutRefPrefix, CurrencyPrefix).foreach { prefix =>
    require(prefix.size == PrefixLength)
  }

  def getAddressPartitionId(address: Address): String = s"$AddressPrefix$address"
  def getAddressNoncePartitionId(address: Address): String = s"$AddressNoncePrefix$address"
  def getUtxoPartitionId(txOutRef: TxOutRef): String = s"$TxOutRefPrefix${txOutRef.txId}${Delimiter}${txOutRef.index}"
  def getCurrencyPartitionId(currency: Currency): String = s"$CurrencyPrefix$currency"

  def toStateKey(partitionId: String): ChimericStateQuery = partitionId.take(PrefixLength) match {
    case AddressPrefix =>
      AddressQuery(partitionId.drop(PrefixLength))

    case AddressNoncePrefix =>
      AddressNonceQuery(partitionId.drop(PrefixLength))

    case TxOutRefPrefix =>
      val utxo = partitionId.drop(PrefixLength).split(Delimiter)
      if (utxo.size != 2) {
        throw new IllegalArgumentException(s"Wrong format for the utxo key ${partitionId}")
      } else {
        UtxoQuery(TxOutRef(utxo.head, utxo.tail.head.toInt))
      }
    case CurrencyPrefix =>
      CurrencyQuery(partitionId.drop(PrefixLength))
    case _ => throw new IllegalStateException(s"Invalid partition key prefix: ${partitionId.take(PrefixLength)}")
  }
}

//Values
sealed trait ChimericStateResult

case class ValueHolder(value: Value) extends ChimericStateResult
case class CreateCurrencyHolder(createCurrency: CreateCurrency) extends ChimericStateResult
case class UtxoResult(value: Value, signingPublicKey: Option[SigningPublicKey]) extends ChimericStateResult
case class AddressResult(value: Value, signingPublicKey: Option[SigningPublicKey]) extends ChimericStateResult
case class NonceResult(nonce: Int) extends ChimericStateResult

//Keys
sealed trait ChimericStateQuery

case class AddressQuery(address: Address) extends ChimericStateQuery
case class AddressNonceQuery(address: Address) extends ChimericStateQuery
case class CurrencyQuery(currency: Currency) extends ChimericStateQuery
case class UtxoQuery(txOutRef: TxOutRef) extends ChimericStateQuery

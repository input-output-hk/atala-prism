package io.iohk.cef.ledger.chimeric

import akka.util.ByteString
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.protobuf.ChimericLedger.ChimericTxFragmentProto.Fragment._
import io.iohk.cef.protobuf.ChimericLedger._
import io.iohk.cef.utils.DecimalProtoUtils

import scala.collection.immutable
import scala.util.Try

object ChimericBlockSerializer {

  def protoValueToValue(protoValue: ChimericValueProto): Value = {
    Value(protoValue.entries.map(entry => entry.currency -> DecimalProtoUtils.fromProto(entry.amount)): _*)
  }

  def valueToProtoValue(value: Value): ChimericValueProto = {
    ChimericValueProto(
      value.iterator.map {
        case (currency, quantity) =>
          ChimericValueEntryProto(currency, DecimalProtoUtils.toProto(quantity))
      }.toSeq
    )
  }

  def protoTxOutRefToTxOutRef(protoTxOutRef: TxOutRefProto): TxOutRef = {
    TxOutRef(protoTxOutRef.txId, protoTxOutRef.index)
  }

  def txOutRefToProtoTxOutRef(txOutRef: TxOutRef): TxOutRefProto = {
    TxOutRefProto(txOutRef.txId, txOutRef.index)
  }

  implicit val serializable: ByteStringSerializable[ChimericLedgerBlock] =
    new ByteStringSerializable[ChimericLedgerBlock] {

      override def decode(bytes: ByteString): Option[ChimericLedgerBlock] = {
        for {
          proto <- Try(ChimericBlockProto.parseFrom(bytes.toArray)).toOption
        } yield {
          val txs: immutable.Seq[ChimericTx] = proto.txs.toList
            .map(txProto =>
              txProto.txFragments.map {
                _.fragment match {
                  case CreateCurrencyWrapper(CreateCurrencyProto(currency)) => CreateCurrency(currency)
                  case DepositWrapper(DepositProto(address, valueProto)) =>
                    Deposit(address, protoValueToValue(valueProto))
                  case FeeWrapper(FeeProto(valueProto)) => Fee(protoValueToValue(valueProto))
                  case InputWrapper(InputProto(txOutRefProto, valueProto)) =>
                    Input(protoTxOutRefToTxOutRef(txOutRefProto), protoValueToValue(valueProto))
                  case MintWrapper(MintProto(valueProto)) => Mint(protoValueToValue(valueProto))
                  case OutputWrapper(OutputProto(valueProto)) => Output(protoValueToValue(valueProto))
                  case WithdrawalWrapper(WithdrawalProto(address, valueProto, nonce)) =>
                    Withdrawal(address, protoValueToValue(valueProto), nonce)
                  case Empty => throw new IllegalArgumentException("Expected a concrete tx fragment but got Empty")
                }
            })
            .map(x => ChimericTx(x))
          Block(new ChimericBlockHeader, txs)
        }
      }

      override def encode(t: ChimericLedgerBlock): ByteString = {
        val proto = ChimericBlockProto(
          ChimericBlockHeaderProto(),
          t.transactions.map(tx => {
            ChimericTxProto(tx.fragments.map(fragment => {
              val fragmentProto = fragment match {
                case Input(TxOutRef(txId, index), value) =>
                  InputWrapper(InputProto(TxOutRefProto(txId, index), valueToProtoValue(value)))
                case Output(value) =>
                  OutputWrapper(OutputProto(valueToProtoValue(value)))
                case Withdrawal(address, value, nonce) =>
                  WithdrawalWrapper(WithdrawalProto(address, valueToProtoValue(value), nonce))
                case Deposit(address, value) =>
                  DepositWrapper(DepositProto(address, valueToProtoValue(value)))
                case Mint(value) =>
                  MintWrapper(MintProto(valueToProtoValue(value)))
                case Fee(value) =>
                  FeeWrapper(FeeProto(valueToProtoValue(value)))
                case CreateCurrency(currency) =>
                  CreateCurrencyWrapper(CreateCurrencyProto(currency))
              }
              ChimericTxFragmentProto(fragmentProto)
            }))
          })
        )
        ByteString(proto.toByteArray)
      }
    }

}

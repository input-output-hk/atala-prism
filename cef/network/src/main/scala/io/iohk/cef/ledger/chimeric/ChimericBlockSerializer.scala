package io.iohk.cef.ledger.chimeric

import akka.util.ByteString
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.protobuf.ChimericLedger.ChimericTxFragmentProto.Fragment._
import io.iohk.cef.protobuf.ChimericLedger._
import io.iohk.cef.utils.DecimalProtoUtils

object ChimericBlockSerializer {

  def protoValueToValue(protoValue: ChimericValueProto): Value = {
    Value(protoValue.entries.map(entry =>
        entry.currency -> DecimalProtoUtils.fromProto(entry.amount)
      ):_*
    )
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

      override def deserialize(bytes: ByteString): ChimericLedgerBlock = {
        val proto = ChimericBlockProto.parseFrom(bytes.toArray)
        val txs: Seq[ChimericTx] = proto.txs.map(txProto =>
          txProto.txFragments.map { txFragment =>
            if (txFragment.fragment.isCreateCurrencyP) {
              CreateCurrency(txFragment.fragment.createCurrencyP.get.currency)
            } else if (txFragment.fragment.isDepositP) {
              val deposit = txFragment.fragment.depositP.get
              Deposit(deposit.address, protoValueToValue(deposit.value))
            } else if (txFragment.fragment.isFeeP) {
              Fee(protoValueToValue(txFragment.fragment.feeP.get.value))
            } else if (txFragment.fragment.isInputP) {
              val input = txFragment.fragment.inputP.get
              Input(protoTxOutRefToTxOutRef(input.txOutRef), protoValueToValue(input.value))
            } else if (txFragment.fragment.isMintP) {
              Mint(protoValueToValue(txFragment.fragment.mintP.get.value))
            } else if (txFragment.fragment.isOutputP) {
              Output(protoValueToValue(txFragment.fragment.outputP.get.value))
            } else if (txFragment.fragment.isWithdrawalP) {
              val withdrawal = txFragment.fragment.withdrawalP.get
              Withdrawal(withdrawal.address, protoValueToValue(withdrawal.value), withdrawal.nonce)
            } else {
              throw new IllegalArgumentException(s"Invalid tx found in proto: ${txFragment.fragment}")
            }
          }
        ).map(x => ChimericTx(x))
        Block(new ChimericBlockHeader, txs)
      }

      override def serialize(t: ChimericLedgerBlock): ByteString = {
        val proto = ChimericBlockProto(
          ChimericBlockHeaderProto(),
          t.transactions.map(tx => {
            ChimericTxProto(tx.fragments.map(fragment => {
              val fragmentProto = fragment match {
                case Input(TxOutRef(txId, index), value) =>
                  InputP(InputProto(TxOutRefProto(txId, index), valueToProtoValue(value)))
                case Output(value) =>
                  OutputP(OutputProto(valueToProtoValue(value)))
                case Withdrawal(address, value, nonce) =>
                  WithdrawalP(WithdrawalProto(address, valueToProtoValue(value), nonce))
                case Deposit(address, value) =>
                  DepositP(DepositProto(address, valueToProtoValue(value)))
                case Mint(value) =>
                  MintP(MintProto(valueToProtoValue(value)))
                case Fee(value) =>
                  FeeP(FeeProto(valueToProtoValue(value)))
                case CreateCurrency(currency) =>
                  CreateCurrencyP(CreateCurrencyProto(currency))
              }
              ChimericTxFragmentProto(fragmentProto)
            }))
          })
        )
        ByteString(proto.toByteArray)
      }
    }

}

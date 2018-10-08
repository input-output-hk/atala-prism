package io.iohk.cef.ledger.chimeric

import akka.util.ByteString
import com.google.protobuf.{ByteString => ProtoByteString}
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.protobuf.ChimericLedger.ChimericTxFragmentProto.Fragment._
import io.iohk.cef.protobuf.ChimericLedger._
import io.iohk.cef.utils.DecimalProtoUtils
import io.iohk.cef.crypto.{SigningPublicKey, Signature}

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

  def signingPublicKeyToProto(signingPublicKey: SigningPublicKey): ProtoByteString = {
    ProtoByteString.copyFrom(signingPublicKey.toByteString.toArray)
  }

  def protoToSigningPublicKey(b: com.google.protobuf.ByteString): SigningPublicKey = {
    SigningPublicKey
      .decodeFrom(ByteString(b.toByteArray))
      .getOrElse(
        throw new IllegalStateException("Signing public key corrupted in protobuf decode step.")
      )
  }

  def signatureToProto(signature: Signature): com.google.protobuf.ByteString = {
    ProtoByteString.copyFrom(signature.toByteString.toArray)
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
                  case DepositWrapper(DepositProto(address, valueProto, signingPublicKey)) =>
                    Deposit(address, protoValueToValue(valueProto), protoToSigningPublicKey(signingPublicKey))
                  case FeeWrapper(FeeProto(valueProto)) => Fee(protoValueToValue(valueProto))
                  case InputWrapper(InputProto(txOutRefProto, valueProto)) =>
                    Input(protoTxOutRefToTxOutRef(txOutRefProto), protoValueToValue(valueProto))
                  case MintWrapper(MintProto(valueProto)) => Mint(protoValueToValue(valueProto))
                  case OutputWrapper(OutputProto(valueProto, signingPublicKey)) =>
                    Output(protoValueToValue(valueProto), protoToSigningPublicKey(signingPublicKey))
                  case WithdrawalWrapper(WithdrawalProto(address, valueProto, nonce)) =>
                    Withdrawal(address, protoValueToValue(valueProto), nonce)
                  case Empty => throw new IllegalArgumentException("Expected a concrete tx fragment but got Empty")
                  case SignatureWrapper(SignatureTxFragmentProto(signatureBytes)) =>
                    ???
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
                case Output(value, signingPublicKey) =>
                  OutputWrapper(OutputProto(valueToProtoValue(value), signingPublicKeyToProto(signingPublicKey)))
                case Withdrawal(address, value, nonce) =>
                  WithdrawalWrapper(WithdrawalProto(address, valueToProtoValue(value), nonce))
                case Deposit(address, value, signingPublicKey) =>
                  DepositWrapper(
                    DepositProto(address, valueToProtoValue(value), signingPublicKeyToProto(signingPublicKey)))
                case Mint(value) =>
                  MintWrapper(MintProto(valueToProtoValue(value)))
                case Fee(value) =>
                  FeeWrapper(FeeProto(valueToProtoValue(value)))
                case CreateCurrency(currency) =>
                  CreateCurrencyWrapper(CreateCurrencyProto(currency))
                case SignatureTxFragment(signature) =>
                  SignatureWrapper(SignatureTxFragmentProto(signatureToProto(signature)))
              }
              ChimericTxFragmentProto(fragmentProto)
            }))
          })
        )
        ByteString(proto.toByteArray)
      }
    }
}

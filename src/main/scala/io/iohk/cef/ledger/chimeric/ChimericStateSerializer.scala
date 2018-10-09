package io.iohk.cef.ledger.chimeric
import akka.util.ByteString
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.protobuf.ChimericLedger.{ChimericStringValueProto, CreateCurrencyProto, NonceProto}
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateValueProto
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateValueProto.Value.{
  Empty,
  StringCreateCurrencyWrapper,
  StringNonceWrapper,
  StringValueWrapper
}

import scala.util.Try

object ChimericStateSerializer {
  implicit val byteStringSerializable = new ByteStringSerializable[ChimericStateResult] {

    override def decode(bytes: ByteString): Option[ChimericStateResult] = {
      for {
        parsed <- Try(ChimericStateValueProto.parseFrom(bytes.toArray)).toOption
      } yield {
        parsed.value match {
          case StringValueWrapper(ChimericStringValueProto(valueStringWrapper)) =>
            ValueHolder(Value(valueStringWrapper.mapValues(BigDecimal(_))))
          case StringCreateCurrencyWrapper(CreateCurrencyProto(currency)) =>
            CreateCurrencyHolder(CreateCurrency(currency))
          case StringNonceWrapper(NonceProto(nonce)) => NonceResult(nonce)
          case Empty => throw new IllegalArgumentException("Expected a string state value wrapper but got Empty")
        }
      }
    }

    override def encode(t: ChimericStateResult): ByteString = {
      val proto = t match {
        case ValueHolder(inner) =>
          StringValueWrapper(ChimericStringValueProto(inner.iterator.map {
            case (key, value) => (key, value.toString())
          }.toMap))

        case NonceResult(nonce) => StringNonceWrapper(NonceProto(nonce))

        case CreateCurrencyHolder(createCurrency) =>
          StringCreateCurrencyWrapper(CreateCurrencyProto(createCurrency.currency))
        case UtxoResult(value, signingPublicKey) =>
          ???
        case AddressResult(value, signingPublicKey) =>
          ???
      }

      ByteString(ChimericStateValueProto(proto).toByteArray)
    }
  }
}

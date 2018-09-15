package io.iohk.cef.ledger.chimeric
import akka.util.ByteString
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.protobuf.ChimericLedger.{ChimericStringValueProto, CreateCurrencyProto, NonceProto}
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateValueProto
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateValueProto.Value.{
  StringCreateCurrencyWrapper,
  StringNonceWrapper,
  StringValueWrapper
}

object ChimericStateSerializer {
  implicit val byteStringSerializable = new ByteStringSerializable[ChimericStateValue] {

    override def deserialize(bytes: ByteString): ChimericStateValue = {
      val parsed = ChimericStateValueProto.parseFrom(bytes.toArray)
      if (parsed.value.isStringCreateCurrencyWrapper) {
        CreateCurrencyHolder(CreateCurrency(parsed.value.stringCreateCurrencyWrapper.get.currency))
      } else if (parsed.value.isStringValueWrapper) {
        ValueHolder(
          Value(parsed.value.stringValueWrapper.get.valueStringWrapper.mapValues(BigDecimal(_)))
        )
      } else {
        throw new IllegalArgumentException(s"Unidentified value: ${parsed.value}")
      }
    }

    override def serialize(t: ChimericStateValue): ByteString = {
      val proto = t match {
        case ValueHolder(inner) =>
          StringValueWrapper(ChimericStringValueProto(inner.iterator.map {
            case (key, value) => (key, value.toString())
          }.toMap))

        case NonceHolder(nonce) => StringNonceWrapper(NonceProto(nonce))

        case CreateCurrencyHolder(createCurrency) =>
          StringCreateCurrencyWrapper(CreateCurrencyProto(createCurrency.currency))
      }

      ByteString(ChimericStateValueProto(proto).toByteArray)
    }
  }
}

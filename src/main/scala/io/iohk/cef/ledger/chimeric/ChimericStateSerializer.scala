package io.iohk.cef.ledger.chimeric
import akka.util.ByteString
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.protobuf.ChimericLedger.{ChimericValueEntryProto, ChimericValueProto, CreateCurrencyProto}
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateStringValueProto.Value.{StringCreateCurrencyWrapper, StringValueWrapper}
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateValueProto.Value.{CreateCurrencyWrapper, ValueWrapper}
import io.iohk.cef.protobuf.ChimericLedgerState.{ChimericStateStringValueProto, ChimericStateValueProto, StringValueProto}
import io.iohk.cef.utils.DecimalProtoUtils

object ChimericStateSerializer {
  val byteStringSerializableUsingString = new ByteStringSerializable[ChimericStateValue] {

    override def deserialize(bytes: ByteString): ChimericStateValue = {
      val parsed = ChimericStateStringValueProto.parseFrom(bytes.toArray)
      if(parsed.value.isStringCreateCurrencyWrapper) {
        CreateCurrencyHolder(CreateCurrency(parsed.value.stringCreateCurrencyWrapper.get.currency))
      } else if(parsed.value.isStringValueWrapper) {
        ValueHolder(
          Value(parsed.value.stringValueWrapper.get.valueStringWrapper.mapValues(BigDecimal(_)))
        )
      } else {
        throw new IllegalArgumentException(s"Unidentified value: ${parsed.value}")
      }
    }

    override def serialize(t: ChimericStateValue): ByteString = {
      val value = t match {
        case ValueHolder(value) =>
          StringValueWrapper(StringValueProto(value.iterator.map{ case (key, value) => (key, value.toString()) }.toMap))
        case CreateCurrencyHolder(createCurrency) =>
          StringCreateCurrencyWrapper(CreateCurrencyProto(createCurrency.currency))
      }
      ByteString(ChimericStateStringValueProto(value).toByteArray)
    }
  }

  val byteStringSerializableUsingDecimal = new ByteStringSerializable[ChimericStateValue] {
    override def deserialize(bytes: ByteString): ChimericStateValue = {
      val parsed = ChimericStateValueProto.parseFrom(bytes.toArray)
      if(parsed.value.isCreateCurrencyWrapper) {
        CreateCurrencyHolder(CreateCurrency(parsed.value.createCurrencyWrapper.get.currency))
      } else if(parsed.value.isValueWrapper) {
        ValueHolder(
          Value(parsed.value.valueWrapper.get.entries.map(entry => {
            (entry.currency, DecimalProtoUtils.fromProto(entry.amount))
          }).toMap)
        )
      } else {
        throw new IllegalArgumentException(s"Unidentified value: ${parsed.value}")
      }
    }

    override def serialize(t: ChimericStateValue): ByteString = {
      val value = t match {
        case ValueHolder(value) =>
          ValueWrapper(ChimericValueProto(value.iterator.map{ case (key, value) => {
            ChimericValueEntryProto(key, DecimalProtoUtils.toProto(value))
          } }.toSeq))
        case CreateCurrencyHolder(createCurrency) =>
          CreateCurrencyWrapper(CreateCurrencyProto(createCurrency.currency))
      }
      ByteString(ChimericStateValueProto(value).toByteArray)
    }
  }
}

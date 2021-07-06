package io.iohk.atala.prism.app.data.local.db.converters

import androidx.room.TypeConverter
import io.iohk.atala.prism.app.data.local.db.model.PayId

class PayIdStatusConverter {
    @TypeConverter
    fun fromIntToPayIdAddressStatus(value: Int): PayId.Status? {
        return when (value) {
            PayId.Status.Registered.ordinal -> PayId.Status.Registered
            PayId.Status.WaitingForResponse.ordinal -> PayId.Status.WaitingForResponse
            else -> null
        }
    }

    @TypeConverter
    fun fromPayIdAddressStatusToInt(value: PayId.Status): Int {
        return value.ordinal
    }
}

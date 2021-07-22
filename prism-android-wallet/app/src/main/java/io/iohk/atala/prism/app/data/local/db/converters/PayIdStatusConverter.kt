package io.iohk.atala.prism.app.data.local.db.converters

import androidx.room.TypeConverter
import io.iohk.atala.prism.app.data.local.db.model.PayId

class PayIdStatusConverter {
    @TypeConverter
    fun fromIntToPayIdAddressStatus(value: Int): PayId.Status? {
        return when (value) {
            PayId.Status.Registered.value -> PayId.Status.Registered
            PayId.Status.WaitingForResponse.value -> PayId.Status.WaitingForResponse
            else -> null
        }
    }

    @TypeConverter
    fun fromPayIdAddressStatusToInt(value: PayId.Status): Int {
        return value.value
    }
}

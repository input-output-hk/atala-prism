package io.iohk.atala.prism.app.data.local.db.converters

import androidx.room.TypeConverter
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress

class PayIdAddressStatusConverter {
    @TypeConverter
    fun fromIntToPayIdAddressStatus(value: Int): PayIdAddress.Status? {
        return when (value) {
            PayIdAddress.Status.Registered.ordinal -> PayIdAddress.Status.Registered
            PayIdAddress.Status.WaitingForResponse.ordinal -> PayIdAddress.Status.WaitingForResponse
            else -> null
        }
    }

    @TypeConverter
    fun fromPayIdAddressStatusToInt(value: PayIdAddress.Status): Int {
        return value.ordinal
    }
}

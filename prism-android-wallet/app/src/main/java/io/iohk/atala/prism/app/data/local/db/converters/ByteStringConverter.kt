package io.iohk.atala.prism.app.data.local.db.converters

import androidx.room.TypeConverter
import com.google.protobuf.ByteString

open class ByteStringConverter {
    @TypeConverter
    fun fromByteArrayToByteString(byteArray: ByteArray): ByteString {
        return ByteString.copyFrom(byteArray)
    }

    @TypeConverter
    fun fromByteStringToByteArray(byteString: ByteString): ByteArray {
        return byteString.toByteArray()
    }
}

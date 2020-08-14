package io.iohk.cvp.utils

import java.nio.ByteBuffer
import java.util.*

class BytesConverterUtil {

    companion object {
        fun getBytesFromUUID(uuid: UUID): ByteArray? {
            val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return bb.array()
        }
    }
}
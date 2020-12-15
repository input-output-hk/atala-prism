package io.iohk.atala.prism.kotlin.util

object BytesOps {
    private val HEX_ARRAY = "0123456789abcdef".toCharArray()

    @ExperimentalUnsignedTypes
    fun bytesToHex(bytes: List<UByte>): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = (bytes[j] and 0xFF.toUByte()).toInt()

            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return hexChars.concatToString()
    }
    
    @ExperimentalUnsignedTypes
    fun hexToBytes(string: String): List<UByte> {
        val result = MutableList(string.length / 2) { UByte.MIN_VALUE }

        for (i in string.indices step 2) {
            val firstIndex = HEX_ARRAY.indexOf(string[i]);
            val secondIndex = HEX_ARRAY.indexOf(string[i + 1]);

            val octet = firstIndex.shl(4).or(secondIndex)
            result[i.shr(1)] = octet.toUByte()
        }
        
        return result
    }
}

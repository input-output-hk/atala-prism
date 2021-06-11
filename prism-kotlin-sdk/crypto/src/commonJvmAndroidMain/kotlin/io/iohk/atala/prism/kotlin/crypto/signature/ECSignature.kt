package io.iohk.atala.prism.kotlin.crypto.signature

import kotlin.experimental.and

actual class ECSignature actual constructor(val data: ByteArray) : ECSignatureCommon() {
    override fun getEncoded(): ByteArray =
        data

    override fun toDer(): ByteArray {
        val size = data.size

        val rb = data.slice(0 until size / 2).toByteArray().dropWhile { it == 0.toByte() }.toMutableList()
        val sb = data.slice(size / 2 until size).toByteArray().dropWhile { it == 0.toByte() }.toMutableList()

        // pad values
        if ((rb[0] and 0x80.toByte()) > 0.toByte()) {
            rb.add(0, 0x0)
        }
        if ((sb[0] and 0x80.toByte()) > 0.toByte()) {
            sb.add(0, 0x0)
        }

        val len = rb.size + sb.size + 4

        val intro = if (len >= 128) byteArrayOf(0x30.toByte(), 0x81.toByte()) else byteArrayOf(0x30.toByte())
        val first = intro + byteArrayOf(len.toByte(), 0x02.toByte(), rb.size.toByte())
        val second = byteArrayOf(0x02.toByte(), sb.size.toByte())

        return first + rb + second + sb
    }
}

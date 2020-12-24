package io.iohk.atala.prism.kotlin.identity.util

import java.util.*

actual object Base64Utils {
    actual fun encode(bytes: List<Byte>): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray())

    actual fun decode(src: String): List<Byte> =
        Base64.getUrlDecoder().decode(src).toList()
}

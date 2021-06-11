package io.iohk.atala.prism.kotlin.protos.util

import java.util.*

actual object Base64Utils {
    actual fun encode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    actual fun decode(src: String): ByteArray =
        Base64.getUrlDecoder().decode(src)
}

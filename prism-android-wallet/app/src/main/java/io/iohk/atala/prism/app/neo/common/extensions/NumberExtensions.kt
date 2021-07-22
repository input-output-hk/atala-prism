package io.iohk.atala.prism.app.neo.common.extensions

enum class NumbersFormat(val format: String) {
    Format02d("%02d")
}
fun Number.format(format: NumbersFormat): String {
    return String.format(format.format, this)
}

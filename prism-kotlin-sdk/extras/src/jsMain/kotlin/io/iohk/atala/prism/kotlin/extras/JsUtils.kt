package io.iohk.atala.prism.kotlin.extras

// These utility methods are need to bridge the gap between Kotlin types and exported JavaScript declarations,
// as there is no convenient way of interacting with kotlin.List from JavaScript.

@JsExport
fun <T> toArray(list: List<T>): Array<T> =
    list.toTypedArray()

@JsExport
fun <T> toList(array: Array<T>): List<T> =
    array.toList()

@JsExport
fun toLong(number: Double): Long =
    number.toLong()

@JsExport
fun toNumber(long: Long): Double =
    long.toDouble()

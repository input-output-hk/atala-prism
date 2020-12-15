package io.iohk.atala.prism.kotlin.crypto.util

import platform.Foundation.*

fun String.toUtf8NsData(): NSData? {
    val nsString = this as NSString
    val decomposedNSString = nsString.decomposedStringWithCompatibilityMapping as NSString
    return decomposedNSString.dataUsingEncoding(NSUTF8StringEncoding)!!
}

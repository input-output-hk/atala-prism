package io.iohk.atala.prism.app.neo.common.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

fun Context.copyToClipBoard(label: String, text: String): Boolean {
    (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.apply {
        this.setPrimaryClip(
            ClipData.newPlainText(label, text)
        )
        return true
    }
    return false
}

package io.iohk.atala.prism.app.neo.common.extensions

import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun MaterialAlertDialogBuilder.setItems(itemsRes: List<Int>, listener: DialogInterface.OnClickListener?): MaterialAlertDialogBuilder {
    val items = itemsRes.map {
        context.getString(it)
    }
    return this.setItems(items.toTypedArray(), listener)
}
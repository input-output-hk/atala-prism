package io.iohk.atala.prism.app.views.utils.components

import android.content.Context
import android.graphics.Typeface
import androidx.appcompat.widget.AppCompatTextView

class ContactDeletionCredentialName(context: Context) : AppCompatTextView(context) {

    init {
        typeface = Typeface.DEFAULT_BOLD;
    }
}
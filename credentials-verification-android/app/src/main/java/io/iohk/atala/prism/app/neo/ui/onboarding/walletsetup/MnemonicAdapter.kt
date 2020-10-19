package io.iohk.atala.prism.app.neo.ui.onboarding.walletsetup

import android.content.Context
import android.widget.*

class MnemonicAdapter(context: Context, resource: Int, textViewId: Int) : ArrayAdapter<String>(context, resource, textViewId) {
    override fun getItem(position: Int): String? {
        val word = super.getItem(position)
        return "${position + 1}. $word"
    }
}
package io.iohk.atala.prism.app.neo.ui.onboarding.walletsetup

import android.annotation.SuppressLint
import io.iohk.cvp.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter

class MnemonicAdapter : BaseRecyclerViewAdapter<String>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseRecyclerViewAdapter.ViewHolder<String> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.neo_row_mnemonic_word, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder(view: View) : BaseRecyclerViewAdapter.ViewHolder<String>(view) {

        private val textView: TextView? = view.findViewById(R.id.text_view_seed)

        @SuppressLint("SetTextI18n")
        override fun bind(data: String) {
            textView?.text = "${adapterPosition + 1}. $data"
        }
    }
}
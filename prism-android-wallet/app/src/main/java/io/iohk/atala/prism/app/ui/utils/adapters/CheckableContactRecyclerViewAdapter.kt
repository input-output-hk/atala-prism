package io.iohk.atala.prism.app.ui.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.cvp.R
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.cvp.databinding.RowCheckableContactBinding
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.model.CheckableData

class CheckableContactRecyclerViewAdapter(private val onSelectedContact: OnSelectItem<CheckableData<Contact>>?) : BaseRecyclerViewAdapter<CheckableData<Contact>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableCredentialViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: RowCheckableContactBinding = DataBindingUtil.inflate(inflater, R.layout.row_checkable_contact, parent, false)
        return CheckableCredentialViewHolder(binding, onSelectedContact)
    }

    /*
    * A ViewHolder Class for [CheckableContactRecyclerViewAdapter]
    * */
    class CheckableCredentialViewHolder(private val binding: RowCheckableContactBinding, private val onSelectedContact: OnSelectItem<CheckableData<Contact>>?) : BaseRecyclerViewAdapter.ViewHolder<CheckableData<Contact>>(binding.root) {

        init {
            binding.root.setOnClickListener { onSelect() }
            binding.checkShare.setOnClickListener { onSelect() }
        }

        override fun bind(checkableContact: CheckableData<Contact>) {
            binding.isChecked = checkableContact.isChecked
            binding.contact = checkableContact.data
        }

        private fun onSelect() {
            data?.let {
                onSelectedContact?.onSelect(it)
            }
        }
    }
}
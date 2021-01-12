package io.iohk.atala.prism.app.views.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.cvp.R
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.cvp.databinding.RowContactBinding
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItem

class ContactsRecyclerViewAdapter(private val onSelectItem: OnSelectItem<Contact>?) : BaseRecyclerViewAdapter<Contact>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: RowContactBinding = DataBindingUtil.inflate(inflater, R.layout.row_contact, parent, false)
        return ContactViewHolder(binding, onSelectItem)
    }

    class ContactViewHolder(private val binding: RowContactBinding, private val onSelectItem: OnSelectItem<Contact>?) : BaseRecyclerViewAdapter.ViewHolder<Contact>(binding.root) {

        init {
            binding.root.setOnClickListener {
                data?.let {
                    onSelectItem?.onSelect(it)
                }
            }
        }

        override fun bind(contact: Contact) {
            binding.contact = contact
        }
    }
}
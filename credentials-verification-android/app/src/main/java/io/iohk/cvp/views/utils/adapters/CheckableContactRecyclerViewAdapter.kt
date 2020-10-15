package io.iohk.cvp.views.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import io.iohk.cvp.R
import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.databinding.RowCheckableContactBinding
import io.iohk.cvp.neo.common.OnSelectItem
import io.iohk.cvp.neo.common.model.CheckableData

class CheckableContactRecyclerViewAdapter(private val onSelectedContact: OnSelectItem<CheckableData<Contact>>?) : RecyclerView.Adapter<CheckableContactRecyclerViewAdapter.CheckableCredentialViewHolder>() {

    private val checkableContacts: MutableList<CheckableData<Contact>> = mutableListOf()

    fun addAll(checkableContacts: List<CheckableData<Contact>>) {
        this.checkableContacts.addAll(checkableContacts)
    }

    fun clear() {
        checkableContacts.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableCredentialViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: RowCheckableContactBinding = DataBindingUtil.inflate(inflater, R.layout.row_checkable_contact, parent, false)
        return CheckableCredentialViewHolder(binding, onSelectedContact)
    }

    override fun onBindViewHolder(holder: CheckableCredentialViewHolder, position: Int) {
        holder.bind(checkableContacts[position])
    }

    override fun getItemCount(): Int {
        return checkableContacts.size
    }


    /*
    * A ViewHolder Class for [CheckableContactRecyclerViewAdapter]
    * */
    class CheckableCredentialViewHolder(private val binding: RowCheckableContactBinding, private val onSelectedContact: OnSelectItem<CheckableData<Contact>>?) : RecyclerView.ViewHolder(binding.root) {

        private var checkableContact: CheckableData<Contact>? = null

        init {
            binding.root.setOnClickListener { onSelect() }
            binding.checkShare.setOnClickListener { onSelect() }
        }

        fun bind(checkableContact: CheckableData<Contact>) {
            binding.isChecked = checkableContact.isChecked
            binding.contact = checkableContact.data
            this.checkableContact = checkableContact
        }

        private fun onSelect() {
            checkableContact?.let {
                onSelectedContact?.onSelect(it)
            }
        }
    }
}
package io.iohk.cvp.views.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import io.iohk.cvp.R
import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.databinding.RowContactBinding
import io.iohk.cvp.neo.common.OnSelectItemAction

class ContactsRecyclerViewAdapter(private val onSelectItemAction: OnSelectItemAction<Contact, Action>?) : RecyclerView.Adapter<ContactsRecyclerViewAdapter.ContactViewHolder>() {

    enum class Action { ActionDelete, ActionDetail }

    private val contacts: MutableList<Contact> = mutableListOf()

    fun addAll(contacts: List<Contact>) {
        this.contacts.addAll(contacts)
    }

    fun clear() {
        contacts.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: RowContactBinding = DataBindingUtil.inflate(inflater, R.layout.row_contact, parent, false)
        return ContactViewHolder(binding, onSelectItemAction)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    class ContactViewHolder(private val binding: RowContactBinding, private val onSelectItemAction: OnSelectItemAction<Contact, Action>?) : RecyclerView.ViewHolder(binding.root) {
        private var contact: Contact? = null

        init {
            binding.deleteContactBtn.setOnClickListener {
                contact?.let {
                    onSelectItemAction?.onSelect(it, Action.ActionDelete)
                }
            }

            binding.root.setOnClickListener {
                contact?.let {
                    onSelectItemAction?.onSelect(it, Action.ActionDetail)
                }
            }
        }

        fun bind(contact: Contact) {
            binding.contact = contact
            this.contact = contact
        }
    }
}
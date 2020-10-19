package io.iohk.atala.prism.app.views.utils.adapters

import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil
import io.iohk.atala.prism.app.data.local.db.model.Contact


class ContactsDiffCallback(private var newConnections: List<Contact>, private var oldConnections: List<Contact>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldConnections.size
    }

    override fun getNewListSize(): Int {
        return newConnections.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldConnections[oldItemPosition].id === newConnections[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldConnections[oldItemPosition] == newConnections[newItemPosition]
    }

    @Nullable
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }

}
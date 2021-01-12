package io.iohk.atala.prism.app.views.utils.adapters

import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil
import io.iohk.atala.prism.app.data.local.db.model.Credential

class CredentialDiffCallback(private var newCredentials: List<Credential>, private var oldCredentials: List<Credential>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldCredentials.size
    }

    override fun getNewListSize(): Int {
        return newCredentials.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldCredentials[oldItemPosition].id === newCredentials[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldCredentials[oldItemPosition] == newCredentials[newItemPosition]
    }

    @Nullable
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }

}
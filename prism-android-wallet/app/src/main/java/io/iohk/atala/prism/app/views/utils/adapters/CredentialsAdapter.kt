package io.iohk.atala.prism.app.views.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.cvp.R
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.cvp.databinding.RowCredentialBinding
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.views.fragments.CredentialUtil

class CredentialsAdapter(private val onSelectItem: OnSelectItem<Credential>? = null) : BaseRecyclerViewAdapter<Credential>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseRecyclerViewAdapter.ViewHolder<Credential> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: RowCredentialBinding = DataBindingUtil.inflate(layoutInflater, R.layout.row_credential, parent, false)
        return ViewHolder(binding, onSelectItem)
    }

    private class ViewHolder(private val binding: RowCredentialBinding, private val onSelectItem: OnSelectItem<Credential>?) : BaseRecyclerViewAdapter.ViewHolder<Credential>(binding.root) {

        init {
            binding.root.setOnClickListener {
                data?.let {
                    onSelectItem?.onSelect(it)
                }
            }
        }

        override fun bind(data: Credential) {
            val ctx = binding.root.context
            val credentialType = data.credentialType
            binding.credentialType.text = CredentialUtil.getName(data, ctx)
            binding.credentialLogo.setImageDrawable(CredentialUtil.getLogo(credentialType, ctx))
            binding.credentialName.text = data.issuerName
        }
    }
}
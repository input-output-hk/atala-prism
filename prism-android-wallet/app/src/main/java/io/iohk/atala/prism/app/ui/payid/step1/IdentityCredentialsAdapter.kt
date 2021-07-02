package io.iohk.atala.prism.app.ui.payid.step1

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import io.iohk.atala.prism.app.ui.main.credentials.CredentialUtil
import io.iohk.cvp.R
import io.iohk.cvp.databinding.RowCheckableCredentialBinding
import io.iohk.cvp.databinding.RowHeaderStyleBBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdentityCredentialsAdapter(
    private val onSelectCredential: OnSelectItem<CheckableData<Credential>>
) : BaseRecyclerViewAdapter<IdentityCredentialsAdapter.ViewType>() {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CREDENTIAL = 1
    }

    sealed class ViewType(val type: Int) {
        data class Header(val title: String) : ViewType(TYPE_HEADER)
        data class Credential(val checkableCredential: CheckableData<io.iohk.atala.prism.app.data.local.db.model.Credential>) : ViewType(TYPE_CREDENTIAL)
    }

    fun updateAllContent(data: HashMap<String, List<CheckableData<Credential>>>) {
        adapterScope.launch {
            val items: MutableList<ViewType> = mutableListOf()
            data.forEach {
                items.add(ViewType.Header(it.key))
                it.value.forEach { credential ->
                    items.add(ViewType.Credential(credential))
                }
            }
            clear()
            addAll(items)
            withContext(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<ViewType> {
        return if (viewType == TYPE_HEADER) {
            buildHeaderViewHolder(parent)
        } else {
            buildCheckableCredentialViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return item.type
    }

    private class HeaderViewHolder(val binding: RowHeaderStyleBBinding) : BaseRecyclerViewAdapter.ViewHolder<ViewType>(binding.root) {
        override fun bind(data: ViewType) {
            (data as? ViewType.Header)?.let {
                binding.title = it.title
            }
        }
    }

    private fun buildHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: RowHeaderStyleBBinding = DataBindingUtil.inflate(layoutInflater, R.layout.row_header_style_b, parent, false)
        return HeaderViewHolder(binding)
    }

    private fun buildCheckableCredentialViewHolder(parent: ViewGroup): CheckableCredentialViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: RowCheckableCredentialBinding = DataBindingUtil.inflate(layoutInflater, R.layout.row_checkable_credential, parent, false)
        return CheckableCredentialViewHolder(binding, onSelectCredential)
    }

    private class CheckableCredentialViewHolder(
        val binding: RowCheckableCredentialBinding,
        val onSelectCredential: OnSelectItem<CheckableData<Credential>>
    ) : BaseRecyclerViewAdapter.ViewHolder<ViewType>(binding.root) {
        init {
            binding.root.setOnClickListener { onClick() }
            binding.checkBox.setOnClickListener { onClick() }
        }
        override fun bind(data: ViewType) {
            (data as? ViewType.Credential)?.checkableCredential?.let { checkableCredential ->
                val ctx = binding.root.context
                val credentialType = checkableCredential.data.credentialType
                binding.credentialType.text = CredentialUtil.getName(checkableCredential.data, ctx)
                binding.credentialLogo.setImageDrawable(CredentialUtil.getLogo(credentialType, ctx))
                binding.credentialName.text = checkableCredential.data.issuerName
                binding.isChecked = checkableCredential.isChecked
            }
        }

        private fun onClick() {
            (data as? ViewType.Credential)?.checkableCredential?.let {
                onSelectCredential.onSelect(it)
            }
        }
    }
}

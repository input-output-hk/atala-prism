package io.iohk.atala.prism.app.ui.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import io.iohk.atala.prism.app.ui.main.credentials.CredentialUtil
import io.iohk.cvp.R
import io.iohk.cvp.databinding.RowProofRequestDialogCredentialBinding

class ProofRequestDialogAdapter(private val onSelectItem: OnSelectItem<Credential>?) : BaseRecyclerViewAdapter<CheckableData<Credential>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseRecyclerViewAdapter.ViewHolder<CheckableData<Credential>> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: RowProofRequestDialogCredentialBinding = DataBindingUtil.inflate(inflater, R.layout.row_proof_request_dialog_credential, parent, false)
        return ViewHolder(binding, onSelectItem)
    }

    private class ViewHolder(
        private val binding: RowProofRequestDialogCredentialBinding,
        private val onSelectItem: OnSelectItem<Credential>?
    ) : BaseRecyclerViewAdapter.ViewHolder<CheckableData<Credential>>(binding.root) {

        init {
            binding.root.setOnClickListener {
                data?.let {
                    onSelectItem?.onSelect(it.data)
                }
            }
        }

        override fun bind(checkableObj: CheckableData<Credential>) {
            val ctx = binding.root.context
            binding.nameTextView.text = CredentialUtil.getName(checkableObj.data, ctx)
            binding.checkbox.isChecked = checkableObj.isChecked
        }
    }
}

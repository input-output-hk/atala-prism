package io.iohk.cvp.views.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.iohk.cvp.R
import io.iohk.cvp.core.enums.CredentialType
import io.iohk.cvp.data.local.db.model.Credential
import io.iohk.cvp.databinding.RowCredentialBinding
import io.iohk.cvp.neo.common.BaseRecyclerViewAdapter
import io.iohk.cvp.neo.common.OnSelectItem

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
            // TODO This is logic inherited from old code we need to check if this logic is correct
            try {
                val credentialType = data.credentialType
                binding.credentialType.text = credentialType
                if (credentialType == CredentialType.REDLAND_CREDENTIAL.value) {
                    binding.credentialType.setText(R.string.credential_government_name)
                    binding.credentialLogo.setImageResource(R.drawable.ic_id_government)
                } else if (credentialType == CredentialType.DEGREE_CREDENTIAL.value) {
                    binding.credentialType.setText(R.string.credential_degree_name)
                    binding.credentialLogo.setImageResource(R.drawable.ic_id_university)
                } else if (credentialType == CredentialType.EMPLOYMENT_CREDENTIAL.value) {
                    binding.credentialType.setText(R.string.credential_employment_name)
                    binding.credentialLogo.setImageResource(R.drawable.ic_id_proof)
                } else {
                    //Certificate Of Insurance
                    binding.credentialType.setText(R.string.credential_insurance_name)
                    binding.credentialLogo.setImageResource(R.drawable.ic_id_insurance)
                }
                binding.credentialName.setText(data.issuerName)
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}
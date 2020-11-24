package io.iohk.atala.prism.app.views.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import io.iohk.atala.prism.app.views.fragments.CredentialUtil
import io.iohk.cvp.databinding.RowActivityLogBinding
import io.iohk.cvp.R

class ActivityLogsAdapter : BaseRecyclerViewAdapter<ActivityHistoryWithContactAndCredential>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<ActivityHistoryWithContactAndCredential> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: RowActivityLogBinding = DataBindingUtil.inflate(inflater, R.layout.row_activity_log, parent, false)
        return ActivityLogViewHolder(binding)
    }

    class ActivityLogViewHolder(private val binding: RowActivityLogBinding) : BaseRecyclerViewAdapter.ViewHolder<ActivityHistoryWithContactAndCredential>(binding.root) {

        override fun bind(data: ActivityHistoryWithContactAndCredential) {
            val ctx = binding.root.context
            val credentialName = if (data.credential != null) CredentialUtil.getName(data.credential, ctx) else ""
            val contactName = data.contact?.name ?: ""
            var iconResourceId = 0
            var title = ""
            var description = ""
            when (data.activityHistory.type) {
                ActivityHistory.Type.CredentialIssued -> {
                    iconResourceId = R.drawable.ic_activity_credential_issued
                    title = credentialName
                    description = ctx.getString(R.string.activity_description_received_from, contactName)
                }
                ActivityHistory.Type.CredentialShared -> {
                    iconResourceId = R.drawable.ic_activity_credential_shared
                    title = credentialName
                    description = ctx.getString(R.string.activity_description_shared_with, contactName)
                }
                ActivityHistory.Type.CredentialRequested -> {
                    iconResourceId = R.drawable.ic_activity_credential_shared
                    title = credentialName
                    description = ctx.getString(R.string.activity_description_requested_by, contactName)
                }
                ActivityHistory.Type.ContactAdded -> {
                    iconResourceId = R.drawable.ic_activity_contact_added
                    title = contactName
                    description = ctx.getString(R.string.activity_description_connected)
                }
                ActivityHistory.Type.CredentialDeleted -> {
                    iconResourceId = R.drawable.ic_activity_removed
                    title = credentialName
                    description = ctx.getString(R.string.activity_description_deleted)
                }
                ActivityHistory.Type.ContactDeleted -> {
                    iconResourceId = R.drawable.ic_activity_removed
                    title = contactName
                    description = ctx.getString(R.string.activity_description_deleted)
                }
            }
            binding.iconImageView.setImageResource(iconResourceId)
            binding.titleTextView.text = title
            binding.descriptionTextView.text = description
            binding.dateTextView.text = dateFormatDDMMYYYY.format(data.activityHistory.date)
        }
    }
}
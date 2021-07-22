package io.iohk.atala.prism.app.ui.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.ui.main.credentials.CredentialUtil
import io.iohk.cvp.R
import io.iohk.cvp.databinding.RowDashboardActivityBinding
import java.text.SimpleDateFormat

class DashboardActivityHistoriesAdapter(private var dateFormat: SimpleDateFormat) : BaseRecyclerViewAdapter<ActivityHistoryWithContactAndCredential>() {

    fun setDateFormat(dateFormat: SimpleDateFormat) {
        this.dateFormat = dateFormat
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<ActivityHistoryWithContactAndCredential> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: RowDashboardActivityBinding = DataBindingUtil.inflate(inflater, R.layout.row_dashboard_activity, parent, false)
        return ActivityLogViewHolder(this, binding)
    }

    class ActivityLogViewHolder(
        private val adapter: DashboardActivityHistoriesAdapter,
        private val binding: RowDashboardActivityBinding
    ) : BaseRecyclerViewAdapter.ViewHolder<ActivityHistoryWithContactAndCredential>(binding.root) {

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
            binding.dateTextView.setDate(data.activityHistory.date, adapter.dateFormat)
        }
    }
}

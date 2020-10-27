package io.iohk.atala.prism.app.views.utils.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import io.iohk.cvp.databinding.RowHeaderBinding
import io.iohk.cvp.R
import io.iohk.cvp.databinding.RowCredentialActivityHistoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This is a Adapter for a list of [ActivityHistoryWithCredential]'s this adapter group all his items by [ActivityHistoryWithCredential.activityHistory.type]
 * and only render [ActivityHistory.Type.CredentialRequested], [ActivityHistory.Type.CredentialIssued], and [ActivityHistory.Type.CredentialShared]
 */
class ContactDetailActivityHistoryAdapter(val context: Context) : BaseRecyclerViewAdapter<ContactDetailActivityHistoryAdapter.ViewType>() {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ACTIVITY_HISTORY = 1

    }

    sealed class ViewType(val type: Int) {
        data class Header(val title: String) : ViewType(TYPE_HEADER)
        data class ActivityHistory(val activityHistoryWithCredential: ActivityHistoryWithCredential) : ViewType(TYPE_ACTIVITY_HISTORY)
    }

    private val requestedHeader = ViewType.Header(context.getString(R.string.requested_credentials))
    private val issuedHeader = ViewType.Header(context.getString(R.string.issued_credentials))
    private val sharedHeader = ViewType.Header(context.getString(R.string.shared_credentials))

    fun updateAllContent(data: List<ActivityHistoryWithCredential>) {
        adapterScope.launch {
            val items: MutableList<ViewType> = mutableListOf()
            val requestedHistory = data.filter {
                it.activityHistory.type == ActivityHistory.Type.CredentialRequested
            }.map {
                ViewType.ActivityHistory(it)
            }
            if (requestedHistory.isNotEmpty()) {
                items.add(requestedHeader)
                items.addAll(requestedHistory)
            }
            val issuedHistory = data.filter {
                it.activityHistory.type == ActivityHistory.Type.CredentialIssued
            }.map {
                ViewType.ActivityHistory(it)
            }
            if (issuedHistory.isNotEmpty()) {
                items.add(issuedHeader)
                items.addAll(issuedHistory)
            }
            val sharedHistory = data.filter {
                it.activityHistory.type == ActivityHistory.Type.CredentialShared
            }.map {
                ViewType.ActivityHistory(it)
            }
            if (sharedHistory.isNotEmpty()) {
                items.add(sharedHeader)
                items.addAll(issuedHistory)
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
            buildActivityHistoryViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return item.type
    }

    private class HeaderViewHolder(val binding: RowHeaderBinding) : BaseRecyclerViewAdapter.ViewHolder<ViewType>(binding.root) {
        override fun bind(data: ViewType) {
            (data as? ViewType.Header)?.let {
                binding.title = it.title
            }
        }
    }

    private fun buildHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: RowHeaderBinding = DataBindingUtil.inflate(layoutInflater, R.layout.row_header, parent, false)
        return HeaderViewHolder(binding)
    }

    private fun buildActivityHistoryViewHolder(parent: ViewGroup): ActivityHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: RowCredentialActivityHistoryBinding = DataBindingUtil.inflate(layoutInflater, R.layout.row_credential_activity_history, parent, false)
        return ActivityHistoryViewHolder(binding)
    }

    private class ActivityHistoryViewHolder(val binding: RowCredentialActivityHistoryBinding) : BaseRecyclerViewAdapter.ViewHolder<ViewType>(binding.root) {
        override fun bind(data: ViewType) {
            (data as? ViewType.ActivityHistory)?.activityHistoryWithCredential?.let {
                val ctx = binding.root.context
                binding.issuerName = it.credential?.issuerName
                val formattedDate = dateFormatDDMMYYYY.format(it.activityHistory.date)
                val dateText = when (it.activityHistory.type) {
                    ActivityHistory.Type.CredentialShared -> ctx.getString(R.string.shared, formattedDate)
                    ActivityHistory.Type.CredentialRequested -> ctx.getString(R.string.requested, formattedDate)
                    else -> ctx.getString(R.string.issued, formattedDate)
                }
                binding.dateString = dateText
            }
        }
    }
}
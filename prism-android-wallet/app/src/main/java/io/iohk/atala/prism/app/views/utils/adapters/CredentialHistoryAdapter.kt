package io.iohk.atala.prism.app.views.utils.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.cvp.R
import io.iohk.cvp.databinding.RowContactActivityHistoryBinding
import io.iohk.cvp.databinding.RowHeaderBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

class CredentialHistoryAdapter(context: Context, private var dateFormat: SimpleDateFormat, private val onSelectItem: OnSelectItem<ActivityHistoryWithContact>? = null) : BaseRecyclerViewAdapter<CredentialHistoryAdapter.ViewType>() {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ACTIVITY_HISTORY = 1

    }

    private val sharedHeader = CredentialHistoryAdapter.ViewType.Header(context.getString(R.string.shared_with))
    private val requestedHeader = CredentialHistoryAdapter.ViewType.Header(context.getString(R.string.requested_by))

    sealed class ViewType(val type: Int) {
        data class Header(val title: String) : ViewType(CredentialHistoryAdapter.TYPE_HEADER)
        data class ActivityHistory(val activityHistoryWithCredential: ActivityHistoryWithContact) : ViewType(CredentialHistoryAdapter.TYPE_ACTIVITY_HISTORY)
    }

    fun setDateFormat(dateFormat: SimpleDateFormat) {
        this.dateFormat = dateFormat
        notifyDataSetChanged()
    }

    fun updateAllContent(data: List<ActivityHistoryWithContact>) {
        adapterScope.launch {
            val items: MutableList<CredentialHistoryAdapter.ViewType> = mutableListOf()

            val sharedHistory = data.filter {
                it.activityHistory.type == ActivityHistory.Type.CredentialShared
            }.map {
                CredentialHistoryAdapter.ViewType.ActivityHistory(it)
            }
            if (sharedHistory.isNotEmpty()) {
                items.add(sharedHeader)
                items.addAll(sharedHistory)
            }

            val requestedHistory = data.filter {
                it.activityHistory.type == ActivityHistory.Type.CredentialRequested
            }.map {
                CredentialHistoryAdapter.ViewType.ActivityHistory(it)
            }
            if (requestedHistory.isNotEmpty()) {
                items.add(requestedHeader)
                items.addAll(requestedHistory)
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

    private fun buildHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: RowHeaderBinding = DataBindingUtil.inflate(layoutInflater, R.layout.row_header, parent, false)
        return HeaderViewHolder(binding)
    }

    private fun buildActivityHistoryViewHolder(parent: ViewGroup): ActivityHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: RowContactActivityHistoryBinding = DataBindingUtil.inflate(layoutInflater, R.layout.row_contact_activity_history, parent, false)
        return ActivityHistoryViewHolder(this, binding, onSelectItem)
    }

    private class HeaderViewHolder(val binding: RowHeaderBinding) : BaseRecyclerViewAdapter.ViewHolder<ViewType>(binding.root) {
        override fun bind(data: ViewType) {
            (data as? ViewType.Header)?.let {
                binding.title = it.title
            }
        }
    }

    private class ActivityHistoryViewHolder(private val adapter: CredentialHistoryAdapter, private val binding: RowContactActivityHistoryBinding, private val onSelectItem: OnSelectItem<ActivityHistoryWithContact>?) : BaseRecyclerViewAdapter.ViewHolder<ViewType>(binding.root) {

        init {
            binding.root.setOnClickListener {
                (data as? ViewType.ActivityHistory)?.activityHistoryWithCredential?.let {
                    onSelectItem?.onSelect(it)
                }
            }
        }

        override fun bind(data: ViewType) {
            (data as? ViewType.ActivityHistory)?.activityHistoryWithCredential?.let {
                binding.contact = it.contact
                binding.formattedDate = adapter.dateFormat.format(it.activityHistory.date)
            }
        }
    }
}
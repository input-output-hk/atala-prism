package io.iohk.atala.prism.app.ui.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.ui.main.credentials.CredentialUtil
import io.iohk.cvp.R
import io.iohk.cvp.databinding.RowNotificationBinding
import java.text.SimpleDateFormat

class NotificationsAdapter(private var dateFormat: SimpleDateFormat, private val onSelectItem: OnSelectItem<ActivityHistoryWithCredential>?) : BaseRecyclerViewAdapter<ActivityHistoryWithCredential>() {

    fun setDateFormat(dateFormat: SimpleDateFormat) {
        this.dateFormat = dateFormat
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseRecyclerViewAdapter.ViewHolder<ActivityHistoryWithCredential> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: RowNotificationBinding = DataBindingUtil.inflate(inflater, R.layout.row_notification, parent, false)
        return ViewHolder(this, binding, onSelectItem)
    }

    private class ViewHolder(private val adapter: NotificationsAdapter, private val binding: RowNotificationBinding, private val onSelectItem: OnSelectItem<ActivityHistoryWithCredential>?) : BaseRecyclerViewAdapter.ViewHolder<ActivityHistoryWithCredential>(binding.root) {

        init {
            binding.root.setOnClickListener {
                data?.let {
                    onSelectItem?.onSelect(it)
                }
            }
        }

        override fun bind(data: ActivityHistoryWithCredential) {
            val ctx = binding.root.context
            data.credential?.let {
                binding.credentialNameTextView.text = it.issuerName
                binding.issuedDateTextView.text = ctx.getString(R.string.received, adapter.dateFormat.format(it.dateReceived))
                binding.credentialTypeTextView.setText(CredentialUtil.getNameResource(it))
                binding.credentialLogoImageView.setImageDrawable(CredentialUtil.getLogo(it.credentialType, ctx))
            }
        }
    }
}
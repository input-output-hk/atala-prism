package io.iohk.atala.prism.app.ui.main.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItemAction
import io.iohk.atala.prism.app.neo.model.DashboardNotification
import io.iohk.cvp.databinding.RowDashboardCardNotificationPayIdBinding
import io.iohk.cvp.databinding.RowDashboardCardNotificationVerifiedCredentialBinding

class CardNotificationsAdapter(private val onSelectItemAction: OnSelectItemAction<DashboardNotification, Action>) : BaseRecyclerViewAdapter<DashboardNotification>() {

    enum class Action { Select, Remove }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<DashboardNotification> {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            DashboardNotification.PayId.identifierCode -> {
                val binding = RowDashboardCardNotificationPayIdBinding.inflate(inflater, parent, false)
                PayIdNotificationViewHolder(binding, onSelectItemAction)
            }
            DashboardNotification.VerifyId.identifierCode -> {
                val binding = RowDashboardCardNotificationVerifiedCredentialBinding.inflate(inflater, parent, false)
                VerifiedIdNotificationViewHolder(binding, onSelectItemAction)
            }
            else -> throw Exception("DashboardNotification Uknown type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].identifierCode
    }

    class PayIdNotificationViewHolder(private val binding: RowDashboardCardNotificationPayIdBinding, onSelectItemAction: OnSelectItemAction<DashboardNotification, Action>) : BaseRecyclerViewAdapter.ViewHolder<DashboardNotification>(binding.root) {
        init {
            binding.selectButton.setOnClickListener {
                data?.let {
                    onSelectItemAction.onSelect(it, Action.Select)
                }
            }
            binding.removeButton.setOnClickListener {
                data?.let {
                    onSelectItemAction.onSelect(it, Action.Remove)
                }
            }
        }
        override fun bind(data: DashboardNotification) {}
    }

    class VerifiedIdNotificationViewHolder(private val binding: RowDashboardCardNotificationVerifiedCredentialBinding, onSelectItemAction: OnSelectItemAction<DashboardNotification, Action>) : BaseRecyclerViewAdapter.ViewHolder<DashboardNotification>(binding.root) {
        init {
            binding.selectButton.setOnClickListener {
                data?.let {
                    onSelectItemAction.onSelect(it, Action.Select)
                }
            }
            binding.removeButton.setOnClickListener {
                data?.let {
                    onSelectItemAction.onSelect(it, Action.Remove)
                }
            }
        }
        override fun bind(data: DashboardNotification) {}
    }
}

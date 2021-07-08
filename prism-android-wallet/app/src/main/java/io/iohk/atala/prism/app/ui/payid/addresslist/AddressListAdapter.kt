package io.iohk.atala.prism.app.ui.payid.addresslist

import android.view.LayoutInflater
import android.view.ViewGroup
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.cvp.databinding.RowPayIdAddressOrPublicKeyBinding

class AddressListAdapter(private val onClipBoarClickListener: OnSelectItem<PayIdAddress>) : BaseRecyclerViewAdapter<PayIdAddress>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<PayIdAddress> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RowPayIdAddressOrPublicKeyBinding.inflate(inflater, parent, false)
        return AddressViewHolder(binding, onClipBoarClickListener)
    }

    class AddressViewHolder(
        private val binding: RowPayIdAddressOrPublicKeyBinding,
        private val onClipBoarClickListener: OnSelectItem<PayIdAddress>
    ) : BaseRecyclerViewAdapter.ViewHolder<PayIdAddress>(binding.root) {
        init {
            binding.clipBoardButton.setOnClickListener {
                data?.let {
                    onClipBoarClickListener.onSelect(it)
                }
            }
        }
        override fun bind(data: PayIdAddress) {
            binding.textView.text = data.address
        }
    }
}

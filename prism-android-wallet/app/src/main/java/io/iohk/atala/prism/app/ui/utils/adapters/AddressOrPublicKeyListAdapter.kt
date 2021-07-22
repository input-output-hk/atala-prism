package io.iohk.atala.prism.app.ui.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.cvp.databinding.RowPayIdAddressOrPublicKeyBinding

class AddressOrPublicKeyListAdapter(private val onClipBoarClickListener: OnSelectItem<String>) : BaseRecyclerViewAdapter<String>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<String> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RowPayIdAddressOrPublicKeyBinding.inflate(inflater, parent, false)
        return AddressViewHolder(binding, onClipBoarClickListener)
    }

    class AddressViewHolder(
        private val binding: RowPayIdAddressOrPublicKeyBinding,
        private val onClipBoarClickListener: OnSelectItem<String>
    ) : BaseRecyclerViewAdapter.ViewHolder<String>(binding.root) {
        init {
            binding.clipBoardButton.setOnClickListener {
                data?.let {
                    onClipBoarClickListener.onSelect(it)
                }
            }
        }
        override fun bind(data: String) {
            binding.textView.text = data
        }
    }
}

package io.iohk.atala.prism.app.ui.utils.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.common.BaseRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import io.iohk.cvp.R
import io.iohk.cvp.databinding.RowCustomDateFormatBinding

class CustomDateFormatAdapter(private val onSelectItem: OnSelectItem<CustomDateFormat>?) : BaseRecyclerViewAdapter<CheckableData<CustomDateFormat>>() {

    private var defaultCustomDateFormat: CustomDateFormat? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<CheckableData<CustomDateFormat>> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: RowCustomDateFormatBinding = DataBindingUtil.inflate(inflater, R.layout.row_custom_date_format, parent, false)
        return CustomDateFormatViewHolder(binding, this, onSelectItem)
    }

    fun setDefaultCustomDateFormat(defaultCustomDateFormat: CustomDateFormat) {
        this.defaultCustomDateFormat = defaultCustomDateFormat
        notifyDataSetChanged()
    }

    class CustomDateFormatViewHolder(
        private val binding: RowCustomDateFormatBinding,
        private val adapter: CustomDateFormatAdapter,
        private val onSelectItem: OnSelectItem<CustomDateFormat>?
    ) : BaseRecyclerViewAdapter.ViewHolder<CheckableData<CustomDateFormat>>(binding.root) {

        init {
            binding.root.setOnClickListener { onSelect() }
        }

        override fun bind(data: CheckableData<CustomDateFormat>) {
            binding.isChecked = data.isChecked
            val dateFormatName = when (data.data) {
                CustomDateFormat.DDMMYYYY -> binding.root.context.getString(R.string.date_format_dd_mm_yyyy)
                CustomDateFormat.MMDDYYYY -> binding.root.context.getString(R.string.date_format_mm_dd_yyyy)
                CustomDateFormat.YYYYMMDD -> binding.root.context.getString(R.string.date_format_yyyy_mm_dd)
            }
            binding.text = if (data.data == adapter.defaultCustomDateFormat) "$dateFormatName ${binding.root.context.getString(R.string.default_indicator)}" else dateFormatName
        }

        private fun onSelect() {
            data?.let {
                onSelectItem?.onSelect(it.data)
            }
        }
    }
}

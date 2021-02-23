package io.iohk.atala.prism.app.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.ui.utils.adapters.CustomDateFormatAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentSettingsDateFormatBinding
import javax.inject.Inject

class SettingsDateFormatFragment : DaggerFragment(), OnSelectItem<CustomDateFormat> {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel: SettingsDateFormatViewModel by lazy {
        ViewModelProviders.of(this, factory).get(SettingsDateFormatViewModel::class.java)
    }

    lateinit var binding: FragmentSettingsDateFormatBinding

    val adapter: CustomDateFormatAdapter by lazy {
        CustomDateFormatAdapter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings_date_format, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureRecyclerView()
        setObservers()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadPreferences()
    }

    override fun onSelect(item: CustomDateFormat) {
        viewModel.selectCustomDateFormat(item)
    }

    private fun configureRecyclerView() {
        binding.dateFormatsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.dateFormatsRecyclerView.adapter = adapter
    }

    private fun setObservers() {
        viewModel.checkableCustomDateFormats.observe(viewLifecycleOwner) {
            adapter.clear()
            adapter.addAll(it)
            adapter.notifyDataSetChanged()
        }
        viewModel.defaultDateFormat.observe(viewLifecycleOwner) {
            adapter.setDefaultCustomDateFormat(it)
        }
        viewModel.preferencesSavedSuccessfully.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    findNavController().popBackStack()
                }
            }
        )
    }
}

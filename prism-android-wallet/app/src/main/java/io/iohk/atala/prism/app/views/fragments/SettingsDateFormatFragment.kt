package io.iohk.atala.prism.app.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.viewmodel.SettingsDateFormatViewModel
import io.iohk.atala.prism.app.viewmodel.SettingsDateFormatViewModelFactory
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator
import io.iohk.atala.prism.app.views.fragments.utils.StackedAppBar
import io.iohk.atala.prism.app.views.utils.adapters.CustomDateFormatAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentSettingsDateFormatBinding
import javax.inject.Inject

class SettingsDateFormatFragment : CvpFragment<SettingsDateFormatViewModel>(), OnSelectItem<CustomDateFormat> {

    @Inject
    lateinit var factory: SettingsDateFormatViewModelFactory

    lateinit var binding: FragmentSettingsDateFormatBinding

    val adapter: CustomDateFormatAdapter by lazy {
        CustomDateFormatAdapter(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, viewId, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureRecyclerView()
        setObservers()
        return binding.root
    }

    override fun getViewModel(): SettingsDateFormatViewModel = ViewModelProvider(this, factory).get(SettingsDateFormatViewModel::class.java)

    override fun getAppBarConfigurator(): AppBarConfigurator {
        setHasOptionsMenu(true)
        return StackedAppBar(R.string.settings_custom_date_format_title)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadPreferences()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            requireActivity().onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getViewId(): Int = R.layout.fragment_settings_date_format

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
        viewModel.preferencesSavedSuccessfully.observe(viewLifecycleOwner, EventWrapperObserver {
            if (it) {
                requireActivity().onBackPressed()
            }
        })
    }
}
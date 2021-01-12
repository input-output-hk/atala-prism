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
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import io.iohk.atala.prism.app.viewmodel.ActivityLogViewModel
import io.iohk.atala.prism.app.viewmodel.ActivityLogViewModelFactory
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator
import io.iohk.atala.prism.app.views.fragments.utils.StackedAppBar
import io.iohk.atala.prism.app.views.utils.adapters.ActivityLogsAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentActivityLogBinding
import javax.inject.Inject

class ActivityLogFragment : CvpFragment<ActivityLogViewModel>() {

    @Inject
    lateinit var viewModelFactory: ActivityLogViewModelFactory

    private lateinit var binding: FragmentActivityLogBinding

    private val adapter: ActivityLogsAdapter by lazy {
        ActivityLogsAdapter(dateFormatDDMMYYYY)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, viewId, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureRecyclerView()
        setObservers()
        return binding.root
    }

    override fun getViewModel(): ActivityLogViewModel {
        return ViewModelProvider(this, viewModelFactory).get(ActivityLogViewModel::class.java)
    }

    override fun getAppBarConfigurator(): AppBarConfigurator {
        setHasOptionsMenu(true)
        return StackedAppBar(R.string.activity_log_title)
    }

    override fun getViewId(): Int = R.layout.fragment_activity_log

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            requireActivity().onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun configureRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setObservers() {
        viewModel.activityHistories.observe(viewLifecycleOwner) {
            adapter.clear()
            adapter.addAll(it)
            adapter.notifyDataSetChanged()
        }
        viewModel.customDateFormat.observe(viewLifecycleOwner) {
            adapter.setDateFormat(it.dateFormat)
        }
    }
}
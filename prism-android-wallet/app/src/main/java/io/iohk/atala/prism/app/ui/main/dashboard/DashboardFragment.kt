package io.iohk.atala.prism.app.ui.main.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.IntentUtils
import io.iohk.atala.prism.app.neo.common.OnSelectItemAction
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYYSimple
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.atala.prism.app.neo.model.DashboardNotification
import io.iohk.atala.prism.app.ui.utils.adapters.DashboardActivityHistoriesAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentDashboardBinding
import javax.inject.Inject

class DashboardFragment : DaggerFragment(), OnSelectItemAction<DashboardNotification, CardNotificationsAdapter.Action> {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: DashboardViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(DashboardViewModel::class.java)
    }

    private lateinit var binding: FragmentDashboardBinding

    private val adapter: DashboardActivityHistoriesAdapter by lazy {
        DashboardActivityHistoriesAdapter(dateFormatDDMMYYYYSimple)
    }

    private val cardNotificationsAdapter: CardNotificationsAdapter by lazy {
        CardNotificationsAdapter(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dashboard, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureRecyclerView()
        setClickListeners()
        setObservers()
        viewModel.loadData()
        // TODO We need to migrate to a "Fragment-owned App Bar" see: https://developer.android.com/guide/fragments/appbar#fragment
        supportActionBar?.hide()
        return binding.root
    }

    private fun configureRecyclerView() {
        binding.lastActivityInclude.activitiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.lastActivityInclude.activitiesRecyclerView.adapter = adapter
        binding.cardsNotificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.cardsNotificationsRecyclerView.adapter = cardNotificationsAdapter
    }

    private fun setObservers() {
        viewModel.lastActivitiesHistories.observe(viewLifecycleOwner) {
            val existActivities = it.isNotEmpty()
            binding.lastActivityInclude.activityLogButton.isEnabled = existActivities
            binding.lastActivityInclude.noActivityImage.visibility = if (existActivities) View.GONE else View.VISIBLE
            binding.lastActivityInclude.activitiesRecyclerView.visibility = if (existActivities) View.VISIBLE else View.GONE
            adapter.clear()
            adapter.addAll(it)
            adapter.notifyDataSetChanged()
        }

        viewModel.customDateFormat.observe(viewLifecycleOwner) {
            adapter.setDateFormat(it.dateFormatSimple)
        }

        viewModel.dashboardCardNotifications.observe(viewLifecycleOwner) {
            cardNotificationsAdapter.clear()
            cardNotificationsAdapter.addAll(it)
            cardNotificationsAdapter.notifyDataSetChanged()
        }
    }

    private fun setClickListeners() {
        binding.profileInclude.viewProfileButton.setOnClickListener { findNavController().navigate(R.id.action_dashboardFragment_to_profileFragment) }
        binding.profileInclude.notificationsButton.setOnClickListener { findNavController().navigate(R.id.action_dashboardFragment_to_notificationsFragment) }
        binding.lastActivityInclude.activityLogButton.setOnClickListener { findNavController().navigate(R.id.action_dashboardFragment_to_activityLogFragment) }
        binding.bannersInclude.demoMoreInfoButton.setOnClickListener { findNavController().navigate(R.id.action_dashboardFragment_to_interactiveDemoActivity) }
        binding.bannersInclude.sendInviteButton.setOnClickListener { share() }
    }

    private fun share() {
        val intent = IntentUtils.intentShareURL(
            "https://play.google.com/store/apps/details?id=io.iohk.cvp",
            getString(R.string.include_dashboard_banners_share_title)
        )
        startActivity(intent)
    }

    override fun onSelect(item: DashboardNotification, action: CardNotificationsAdapter.Action?) {
        when (action) {
            CardNotificationsAdapter.Action.Remove -> viewModel.removeDashboardNotification(item)
            CardNotificationsAdapter.Action.Select -> {
                when (item) {
                    DashboardNotification.PayId -> findNavController().navigate(R.id.action_dashboardFragment_to_payIdObtainingNavActivity)
                    DashboardNotification.VerifyId -> Toast.makeText(requireContext(), "To be implemented", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

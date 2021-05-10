package io.iohk.atala.prism.app.ui.main.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.atala.prism.app.ui.utils.adapters.NotificationsAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentNotificationsBinding
import javax.inject.Inject

class NotificationsFragment : DaggerFragment(), OnSelectItem<ActivityHistoryWithCredential> {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: NotificationsViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(NotificationsViewModel::class.java)
    }

    lateinit var binding: FragmentNotificationsBinding

    private val adapter: NotificationsAdapter by lazy { NotificationsAdapter(dateFormatDDMMYYYY, this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_notifications, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        configureRecyclerView()
        // TODO We need to migrate to a "Fragment-owned App Bar" see: https://developer.android.com/guide/fragments/appbar#fragment
        supportActionBar?.show()
        return binding.root
    }

    private fun setObservers() {
        viewModel.notifications.observe(viewLifecycleOwner) {
            adapter.clear()
            adapter.addAll(it)
            adapter.notifyDataSetChanged()
        }
        viewModel.customDateFormat.observe(viewLifecycleOwner) {
            adapter.setDateFormat(it.dateFormat)
        }
    }

    override fun onSelect(item: ActivityHistoryWithCredential) {
        item.credential?.credentialId?.let { credentialId ->
            val direction = NotificationsFragmentDirections.actionNotificationsFragmentToCredentialDetailNavigation(credentialId)
            findNavController().navigate(direction)
        }
    }

    private fun configureRecyclerView() {
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRecyclerView.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_activity_log) {
            findNavController().navigate(R.id.action_notificationsFragment_to_activityLogFragment)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

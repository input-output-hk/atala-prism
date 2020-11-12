package io.iohk.atala.prism.app.views.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.utils.ActivitiesRequestCodes
import io.iohk.atala.prism.app.utils.IntentDataConstants
import io.iohk.atala.prism.app.viewmodel.NotificationsViewModel
import io.iohk.atala.prism.app.viewmodel.NotificationsViewModelFactory
import io.iohk.atala.prism.app.views.fragments.AcceptConnectionDialogFragment.Companion.build
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator
import io.iohk.atala.prism.app.views.fragments.utils.RootAppBar
import io.iohk.atala.prism.app.views.utils.adapters.NotificationsAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentNotificationsBinding
import javax.inject.Inject

class NotificationsFragment : CvpFragment<NotificationsViewModel>(), OnSelectItem<ActivityHistoryWithCredential> {

    @Inject
    lateinit var viewModelFactory: NotificationsViewModelFactory

    lateinit var binding: FragmentNotificationsBinding

    private val adapter: NotificationsAdapter by lazy { NotificationsAdapter(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, viewId, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.setScanQrBtnOnClick {
            scanQr()
        }
        setObservers()
        configureRecyclerView()
        return binding.root
    }

    override fun getViewModel(): NotificationsViewModel {
        return ViewModelProvider(this, viewModelFactory).get(NotificationsViewModel::class.java)
    }

    override fun getAppBarConfigurator(): AppBarConfigurator {
        return RootAppBar(R.string.notifications)
    }

    override fun getViewId(): Int {
        return R.layout.fragment_notifications
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra(IntentDataConstants.QR_RESULT)?.let {
                // TODO momentary solution, the use of "safeArgs" will be implemented
                val dialog = build(it)
                dialog.show(requireActivity().supportFragmentManager, null)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setObservers() {
        viewModel.notifications.observe(viewLifecycleOwner) {
            adapter.clear()
            adapter.addAll(it)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onSelect(item: ActivityHistoryWithCredential) {
        // TODO This is logic inherited from old code we need to use the navigation components
        item.credential?.let {
            val credentialFragment = CredentialDetailFragment()
            credentialFragment.setCredential(it)
            navigator.showFragmentOnTop(
                    requireActivity().supportFragmentManager, credentialFragment)
        }
    }

    private fun configureRecyclerView() {
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRecyclerView.adapter = adapter
    }
}
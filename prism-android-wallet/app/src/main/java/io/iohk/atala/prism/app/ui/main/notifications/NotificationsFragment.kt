package io.iohk.atala.prism.app.ui.main.notifications

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.neo.common.IntentUtils
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import io.iohk.atala.prism.app.neo.common.extensions.buildActivityResultLauncher
import io.iohk.atala.prism.app.neo.common.extensions.buildRequestPermissionLauncher
import io.iohk.atala.prism.app.utils.IntentDataConstants
import io.iohk.atala.prism.app.ui.utils.adapters.NotificationsAdapter
import io.iohk.atala.prism.app.utils.PermissionUtils
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentNotificationsBinding
import javax.inject.Inject

class NotificationsFragment : DaggerFragment(), OnSelectItem<ActivityHistoryWithCredential> {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel:NotificationsViewModel by lazy {
        ViewModelProviders.of(this,viewModelFactory).get(NotificationsViewModel::class.java)
    }

    // Launcher for QrCodeScannerActivity
    private val qrActivityResultLauncher = buildActivityResultLauncher{ activityResult ->
        if(activityResult.resultCode == Activity.RESULT_OK && activityResult.data?.hasExtra(IntentDataConstants.QR_RESULT) == true){
            val token = activityResult.data!!.getStringExtra(IntentDataConstants.QR_RESULT)!!
            val direction = NotificationsFragmentDirections.actionNotificationsFragmentToAcceptConnectionDialogFragment(token)
            findNavController().navigate(direction)
        }
    }

    private val cameraPermissionLauncher = buildRequestPermissionLauncher { permissionGranted ->
        if(permissionGranted) showQRScanner()
    }

    lateinit var binding: FragmentNotificationsBinding

    private val adapter: NotificationsAdapter by lazy { NotificationsAdapter(dateFormatDDMMYYYY, this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_notifications, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.setScanQrBtnOnClick { showQRScanner() }
        setObservers()
        configureRecyclerView()
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
        item.credential?.credentialId?.let { credentialId->
            val direction = NotificationsFragmentDirections.actionNotificationsFragmentToCredentialDetailNavigation(credentialId)
            findNavController().navigate(direction)
        }
    }

    private fun configureRecyclerView() {
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRecyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) = inflater.inflate(R.menu.notifications_menu,menu)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_activity_log) {
            findNavController().navigate(R.id.action_notificationsFragment_to_activityLogFragment);
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showQRScanner(){
        if (PermissionUtils.checkIfAlreadyHavePermission(requireContext(), Manifest.permission.CAMERA)) {
            val intent = IntentUtils.intentQRCodeScanner(requireContext())
            qrActivityResultLauncher.launch(intent)
        }else{
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
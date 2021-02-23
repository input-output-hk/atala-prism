package io.iohk.atala.prism.app.ui.main.contacts

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.neo.common.IntentUtils
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.extensions.buildActivityResultLauncher
import io.iohk.atala.prism.app.neo.common.extensions.buildRequestPermissionLauncher
import io.iohk.atala.prism.app.ui.commondialogs.AddQrCodeDialogFragment
import io.iohk.atala.prism.app.ui.utils.adapters.ContactsRecyclerViewAdapter
import io.iohk.atala.prism.app.utils.IntentDataConstants
import io.iohk.atala.prism.app.utils.PermissionUtils
import io.iohk.cvp.BuildConfig
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentContactsBinding
import javax.inject.Inject

class ContactsFragment : DaggerFragment(), OnSelectItem<Contact> {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: ContactsViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ContactsViewModel::class.java)
    }

    // Launcher for QrCodeScannerActivity
    private val qrActivityResultLauncher = buildActivityResultLauncher { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK && activityResult.data?.hasExtra(IntentDataConstants.QR_RESULT) == true) {
            val token = activityResult.data!!.getStringExtra(IntentDataConstants.QR_RESULT)!!
            navigateToAcceptConnectionDialog(token)
        }
    }

    private val cameraPermissionLauncher = buildRequestPermissionLauncher { permissionGranted ->
        if (permissionGranted) showQRScanner()
    }

    private lateinit var binding: FragmentContactsBinding

    private val adapter = ContactsRecyclerViewAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(AddQrCodeDialogFragment.KEY_REQUEST_ADD_QR) { _, bundle ->
            if (bundle.containsKey(AddQrCodeDialogFragment.KEY_RESULT_CODE)) {
                val token = bundle.getString(AddQrCodeDialogFragment.KEY_RESULT_CODE)!!
                navigateToAcceptConnectionDialog(token)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_contacts, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.setScanQrBtnOnClick {
            showQRScanner()
        }
        configureRecyclerView()
        initObservers()
        return binding.root
    }

    private fun configureRecyclerView() {
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.contactsRecyclerView.adapter = adapter
    }

    private fun initObservers() {
        viewModel.contacts.observe(
            viewLifecycleOwner,
            { contacts ->
                adapter.clear()
                adapter.addAll(contacts)
                adapter.notifyDataSetChanged()
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) = inflater.inflate(R.menu.contacts_menu, menu)

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (BuildConfig.DEBUG) menu.findItem(R.id.action_debug_add_new_connection)?.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_connection -> {
                showQRScanner()
                true
            }
            R.id.action_debug_add_new_connection -> {
                findNavController().navigate(R.id.action_contactsFragment_to_addQrCodeDialogFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSelect(item: Contact) {
        val direction = ContactsFragmentDirections.actionContactsFragmentToContactDetailNavigation(item.id)
        findNavController().navigate(direction)
    }

    private fun showQRScanner() {
        if (PermissionUtils.checkIfAlreadyHavePermission(requireContext(), Manifest.permission.CAMERA)) {
            val intent = IntentUtils.intentQRCodeScanner(requireContext())
            qrActivityResultLauncher.launch(intent)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun navigateToAcceptConnectionDialog(token: String) {
        val direction = ContactsFragmentDirections.actionContactsFragmentToAcceptConnectionDialogFragment(token)
        findNavController().navigate(direction)
    }
}

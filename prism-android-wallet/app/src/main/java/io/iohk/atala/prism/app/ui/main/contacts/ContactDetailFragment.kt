package io.iohk.atala.prism.app.ui.main.contacts

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import io.iohk.atala.prism.app.neo.common.extensions.KEY_RESULT
import io.iohk.atala.prism.app.ui.utils.adapters.ContactDetailActivityHistoryAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoFragmentContactDetailBinding
import javax.inject.Inject

class ContactDetailFragment : DaggerFragment() {

    private val args:ContactDetailFragmentArgs by navArgs()

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel:ContactDetailViewModel by lazy {
        ViewModelProviders.of(this,viewModelFactory).get(ContactDetailViewModel::class.java)
    }

    private lateinit var binding: NeoFragmentContactDetailBinding

    private val adapter by lazy {
        ContactDetailActivityHistoryAdapter(requireContext(), dateFormatDDMMYYYY)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_contact_detail, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureRecyclerView()
        setObservers()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.fetchContact(args.contactId)
        // Handle DeleteContactAlertDialogFragment result
        setFragmentResultListener(DeleteContactAlertDialogFragment.REQUEST_DELETE_CONTACT){ requestKey, bundle ->
            if(bundle.getInt(KEY_RESULT) == Activity.RESULT_OK){
                findNavController().popBackStack()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) = inflater.inflate(R.menu.contact_detail_menu,menu)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_contact -> {
                val direction = ContactDetailFragmentDirections.actionContactDetailFragmentToDeleteContactAlertDialogFragment(args.contactId)
                Navigation.findNavController(requireView()).navigate(direction)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setObservers() {
        viewModel.credentialActivityHistories.observe(viewLifecycleOwner) { credentialActivityHistories ->
            adapter.updateAllContent(credentialActivityHistories)
        }
        viewModel.customDateFormat.observe(viewLifecycleOwner) {
            adapter.setDateFormat(it.dateFormat)
        }
    }

    private fun configureRecyclerView() {
        binding.activityList.adapter = adapter
        binding.activityList.layoutManager = LinearLayoutManager(requireContext())
    }
}
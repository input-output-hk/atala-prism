package io.iohk.atala.prism.app.views.fragments

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import io.iohk.atala.prism.app.neo.common.OnSuccess
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import io.iohk.atala.prism.app.viewmodel.ContactDetailViewModel
import io.iohk.atala.prism.app.viewmodel.ContactDetailViewModelFactory
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator
import io.iohk.atala.prism.app.views.fragments.utils.StackedAppBar
import io.iohk.atala.prism.app.views.utils.adapters.ContactDetailActivityHistoryAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoFragmentContactDetailBinding
import javax.inject.Inject

class ContactDetailFragment : CvpFragment<ContactDetailViewModel>() {

    companion object {
        fun build(contactId: Long): ContactDetailFragment {
            val fragment = ContactDetailFragment()
            fragment.contactId = contactId
            return fragment
        }
    }


    @Inject
    lateinit var viewModelFactory: ContactDetailViewModelFactory

    private lateinit var binding: NeoFragmentContactDetailBinding

    private var contactId: Long = -1

    private val adapter by lazy {
        ContactDetailActivityHistoryAdapter(requireContext(), dateFormatDDMMYYYY)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, viewId, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureRecyclerView()
        setObservers()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.fetchContact(contactId)
    }

    override fun getViewModel(): ContactDetailViewModel {
        return ViewModelProvider(this, viewModelFactory).get(ContactDetailViewModel::class.java)
    }

    override fun getAppBarConfigurator(): AppBarConfigurator {
        setHasOptionsMenu(true)
        return StackedAppBar(R.string.contact_detail)
    }

    override fun getViewId(): Int {
        return R.layout.neo_fragment_contact_detail
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val deleteContactMenuItem = menu.findItem(R.id.action_delete_contact)
        deleteContactMenuItem.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
                return true
            }
            R.id.action_delete_contact -> {
                DeleteContactAlertDialogFragment
                        .build(contactId.toInt(), object : OnSuccess<Boolean> {
                            override fun onSuccess(data: Boolean) {
                                requireActivity().onBackPressed()
                            }
                        })
                        .show(requireActivity().supportFragmentManager, null)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
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
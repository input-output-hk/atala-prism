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
import io.iohk.atala.prism.app.core.enums.CredentialType
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.viewmodel.CredentialHistoryViewModel
import io.iohk.atala.prism.app.viewmodel.CredentialHistoryViewModelFactory
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator
import io.iohk.atala.prism.app.views.fragments.utils.StackedAppBar
import io.iohk.atala.prism.app.views.utils.adapters.CredentialHistoryAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoFragmentCredentialHistoryBinding
import javax.inject.Inject

class CredentialHistoryFragment : CvpFragment<CredentialHistoryViewModel>(), OnSelectItem<ActivityHistoryWithContact> {

    companion object {
        fun build(credentialId: String): CredentialHistoryFragment {
            val fragment = CredentialHistoryFragment()
            fragment.credentialId = credentialId
            return fragment
        }
    }

    @Inject
    lateinit var viewModelFactory: CredentialHistoryViewModelFactory

    private lateinit var binding: NeoFragmentCredentialHistoryBinding

    private var credentialId: String? = null

    private val activitiesHistoriesAdapter by lazy { CredentialHistoryAdapter(requireContext(), onSelectItem = this) }

    // TODO this is a hack to support the behavior of [CvpFragment] and the current custom navigation system, when we migrate to a native Android navigation and [CvpFragment] inheritance is removed this has to be deleted
    private var titleResource = R.string.home_title

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, viewId, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        configureList()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        credentialId?.let {
            viewModel.fetchData(it)
        }
    }

    override fun getViewModel(): CredentialHistoryViewModel {
        return ViewModelProvider(this, viewModelFactory).get(CredentialHistoryViewModel::class.java)
    }

    override fun getAppBarConfigurator(): AppBarConfigurator {
        setHasOptionsMenu(true)
        return StackedAppBar(titleResource)
    }

    override fun getViewId(): Int {
        return R.layout.neo_fragment_credential_history
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            requireActivity().onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setObservers() {
        viewModel.credential.observe(viewLifecycleOwner) {
            // TODO this is a hack to support the behavior of [CvpFragment] and the current custom navigation system, when we migrate to a native Android navigation and [CvpFragment] inheritance is removed this has to be deleted
            titleResource = CredentialUtil.getNameResource(it)
            setActionBarTitle(titleResource)
            binding.credentialLogo.setImageDrawable(CredentialUtil.getLogo(it.credentialType, requireContext()))
        }
        viewModel.activityHistories.observe(viewLifecycleOwner) {
            activitiesHistoriesAdapter.updateAllContent(it)
        }
    }

    private fun configureList() {
        binding.activityList.layoutManager = LinearLayoutManager(requireContext())
        binding.activityList.adapter = activitiesHistoriesAdapter
    }

    override fun onSelect(item: ActivityHistoryWithContact) {
        item.contact?.let {
            val fragment = ContactDetailFragment.build(it.id)
            navigator.showFragmentOnTop(requireActivity().supportFragmentManager, fragment)
        }
    }
}
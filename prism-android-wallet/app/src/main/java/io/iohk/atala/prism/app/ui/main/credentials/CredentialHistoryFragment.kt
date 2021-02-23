package io.iohk.atala.prism.app.ui.main.credentials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.atala.prism.app.ui.utils.adapters.CredentialHistoryAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoFragmentCredentialHistoryBinding
import javax.inject.Inject

class CredentialHistoryFragment : DaggerFragment(), OnSelectItem<ActivityHistoryWithContact> {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val args:CredentialHistoryFragmentArgs by navArgs()

    private val viewModel:CredentialHistoryViewModel by lazy {
        ViewModelProviders.of(this,viewModelFactory).get(CredentialHistoryViewModel::class.java)
    }

    private lateinit var binding: NeoFragmentCredentialHistoryBinding

    private val activitiesHistoriesAdapter by lazy { CredentialHistoryAdapter(requireContext(), dateFormatDDMMYYYY, onSelectItem = this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_credential_history, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        configureList()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.fetchData(args.credentialId)
    }

    private fun setObservers() {
        viewModel.credential.observe(viewLifecycleOwner) {
            val title = getString(CredentialUtil.getNameResource(it))
            findNavController().currentDestination?.label = title
            supportActionBar?.title = title
            binding.credentialLogo.setImageDrawable(CredentialUtil.getLogo(it.credentialType, requireContext()))
        }
        viewModel.activityHistories.observe(viewLifecycleOwner) {
            activitiesHistoriesAdapter.updateAllContent(it)
        }
        viewModel.customDateFormat.observe(viewLifecycleOwner) {
            activitiesHistoriesAdapter.setDateFormat(it.dateFormat)
        }
    }

    private fun configureList() {
        binding.activityList.layoutManager = LinearLayoutManager(requireContext())
        binding.activityList.adapter = activitiesHistoriesAdapter
    }

    override fun onSelect(item: ActivityHistoryWithContact) {
        item.contact?.let {
            val direction = CredentialHistoryFragmentDirections.actionCredentialHistoryFragmentToContactDetailNavigation(it.id)
            findNavController().navigate(direction)
        }
    }
}
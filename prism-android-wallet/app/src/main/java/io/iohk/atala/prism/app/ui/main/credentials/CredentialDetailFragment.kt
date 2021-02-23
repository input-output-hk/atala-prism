package io.iohk.atala.prism.app.ui.main.credentials

import android.app.Activity
import android.os.Bundle
import android.util.Base64
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
import androidx.navigation.fragment.navArgs
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.extensions.KEY_RESULT
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentCredentialDetailBinding
import javax.inject.Inject

class CredentialDetailFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: CredentialDetailViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(CredentialDetailViewModel::class.java)
    }

    private val args: CredentialDetailFragmentArgs by navArgs()

    lateinit var binding: FragmentCredentialDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        // Handle DeleteCredentialDialogFragment result
        setFragmentResultListener(DeleteCredentialDialogFragment.REQUEST_DELETE_CREDENTIAL) { _, bundle ->
            if (bundle.getInt(KEY_RESULT) == Activity.RESULT_OK) {
                findNavController().popBackStack()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_credential_detail, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        viewModel.fetchCredentialInfo(args.credentialId)
        setObservers()
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.credential_detail_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_credential_history -> {
                val direction = CredentialDetailFragmentDirections.actionCredentialDetailFragmentToCredentialHistoryFragment(args.credentialId)
                findNavController().navigate(direction)
                true
            }
            R.id.action_delete_credential -> {
                val direction = CredentialDetailFragmentDirections.actionCredentialDetailFragmentToDeleteCredentialDialogFragment(args.credentialId)
                findNavController().navigate(direction)
                true
            }
            R.id.action_share_credential -> {
                val direction = CredentialDetailFragmentDirections.actionCredentialDetailFragmentToShareCredentialDialogFragment(args.credentialId)
                findNavController().navigate(direction)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setObservers() {
        viewModel.credential.observe(viewLifecycleOwner) { credential ->
            fillWebView(credential)
            val title = getString(CredentialUtil.getNameResource(credential))
            findNavController().currentDestination?.label = title
            supportActionBar?.title = title
        }
    }

    private fun fillWebView(credential: Credential) {
        val credentialHtmlString = CredentialUtil.getHtml(credential)
        val encodedHtml = Base64.encodeToString(credentialHtmlString.toByteArray(), Base64.NO_PADDING)
        binding.webView.loadData(encodedHtml, "text/html", "base64")
    }
}

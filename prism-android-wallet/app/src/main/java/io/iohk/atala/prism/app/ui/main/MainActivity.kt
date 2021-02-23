package io.iohk.atala.prism.app.ui.main

import android.accounts.Account
import android.content.ContentResolver
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.android.support.DaggerAppCompatActivity
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.IntentUtils
import io.iohk.atala.prism.app.neo.common.extensions.setupWithNavController
import io.iohk.atala.prism.app.neo.sync.AuthenticatorService
import io.iohk.atala.prism.app.ui.commondialogs.ProofRequestDialogFragment
import io.iohk.cvp.R
import io.iohk.cvp.databinding.ActivityMainBinding
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    val viewModel:MainViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)
    }

    // TODO this will be removed when the GRPC data stream is implemented
    private val genericSyncAccount:Account by lazy {
        AuthenticatorService.buildGenericAccountForSync(this)
    }

    lateinit var binding:ActivityMainBinding

    private var currentNavController: LiveData<NavController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        if (savedInstanceState == null) {
            setupBottomNavigationBar()
            binding.bottomNavigationView.selectedItemId = R.id.notifications_navigation
        }
        setObservers()
        viewModel.checkSecuritySettings()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {

        val navGraphIds = listOf(
                R.navigation.credentials_navigation,
                R.navigation.contacts_navigation,
                R.navigation.notifications_navigation,
                R.navigation.profile_navigation,
                R.navigation.settings_navigation
        )

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = binding.bottomNavigationView.setupWithNavController(
                navGraphIds = navGraphIds,
                fragmentManager = supportFragmentManager,
                containerId = R.id.nav_host_container,
                intent = intent
        )

        controller.observe(this) { navController ->
            // Whenever the selected controller changes, setup the action bar.
            setupActionBarWithNavController(navController)
            // Notify the state of the notification view so that the layout updates the state of the notification button a [FloatingActionButton]
            binding.notificationSectionSelected = navController.graph.id == R.id.notifications_navigation
        }
        currentNavController = controller

        binding.fab.setOnClickListener {
            binding.bottomNavigationView.selectedItemId = R.id.notifications_navigation
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    private fun setObservers(){
        // TODO this will be removed when the GRPC data stream is implemented
        // Handle a sync request
        viewModel.requestSync.observe(this, EventWrapperObserver {
            if (it) requestForSync()
        })
        // Handle  when exist a message with a proof requests
        viewModel.proofRequest.observe(this, EventWrapperObserver { proofRequest ->
            ProofRequestDialogFragment.build(proofRequest.id).show(supportFragmentManager, null)
        })
        // Handle security view
        viewModel.securityViewShouldBeVisible.observe(this, EventWrapperObserver { showSecurityView ->
            if(showSecurityView) {
                startActivity(IntentUtils.intentUnlockScreen(this))
            }
        })
    }

    // TODO this will be removed when the GRPC data stream is implemented
    private fun requestForSync() {
        val settingsBundle = Bundle()
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true)
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        /*
         * Request the sync for the generic account, authority, and manual sync settings
         */
        ContentResolver.requestSync(genericSyncAccount, AuthenticatorService.ACCOUNT_AUTHORITY, settingsBundle)
    }
}
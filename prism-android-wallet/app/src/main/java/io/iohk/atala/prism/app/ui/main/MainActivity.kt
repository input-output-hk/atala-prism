package io.iohk.atala.prism.app.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.android.support.DaggerAppCompatActivity
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.IntentUtils
import io.iohk.atala.prism.app.neo.common.extensions.setupWithNavController
import io.iohk.atala.prism.app.ui.commondialogs.ProofRequestDialogFragment
import io.iohk.cvp.R
import io.iohk.cvp.databinding.ActivityMainBinding
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    val viewModel: MainViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)
    }

    lateinit var binding: ActivityMainBinding

    private var currentNavController: LiveData<NavController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        if (savedInstanceState == null) {
            setupBottomNavigationBar()
            binding.bottomNavigationView.selectedItemId = R.id.dashboard_navigation
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
    @SuppressLint("ClickableViewAccessibility")
    private fun setupBottomNavigationBar() {

        val navGraphIds = listOf(
            R.navigation.credentials_navigation,
            R.navigation.contacts_navigation,
            R.navigation.dashboard_navigation,
            R.navigation.services_navigation,
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
            binding.dashboardSectionSelected = navController.graph.id == R.id.dashboard_navigation
        }
        currentNavController = controller

        binding.fab.setOnClickListener {
            binding.bottomNavigationView.selectedItemId = R.id.dashboard_navigation
        }

        // Disable Services Tab
        binding.bottomNavigationView.setItemOnTouchListener(R.id.services_navigation) { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.available_soon)
                    .setPositiveButton(R.string.accept, null)
                    .show()
            }
            return@setItemOnTouchListener true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    private fun setObservers() {
        // Handle  when exist a message with a proof requests
        viewModel.proofRequest.observe(
            this,
            EventWrapperObserver { proofRequest ->
                ProofRequestDialogFragment.build(proofRequest.id).show(supportFragmentManager, null)
            }
        )
        // Handle security view
        viewModel.securityViewShouldBeVisible.observe(
            this,
            EventWrapperObserver { showSecurityView ->
                if (showSecurityView) {
                    startActivity(IntentUtils.intentUnlockScreen(this))
                }
            }
        )
    }
}

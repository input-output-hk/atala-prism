package io.iohk.atala.prism.app.neo.ui.launch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.DaggerAppCompatActivity
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoActivityLaunchBinding
import io.iohk.atala.prism.app.neo.ui.onboarding.OnBoardingNavActivity
import io.iohk.atala.prism.app.ui.main.MainActivity
import io.iohk.atala.prism.app.ui.utils.ForegroundBackgroundListener
import javax.inject.Inject

class LaunchActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory


    private val viewModel: LaunchViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(LaunchViewModel::class.java)
    }

    private lateinit var binding: NeoActivityLaunchBinding

    /**
     * [Handler] for delayed navigation
     * */
    private var handler: Handler? = null

    private val navigationDelayInMilliseconds = 1500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.neo_activity_launch)
        binding.lifecycleOwner = this
        configureObservers()

        // TODO: Refactor "isFirstLaunch" for now this prop its for handle a Security PIN Layout
        ForegroundBackgroundListener.isFirstLaunch = true
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkSession()
    }

    private fun configureObservers() {
        // Observe when exist a stored session
        viewModel.sessionDataHasStored.observe(this, Observer {
            handler?.removeCallbacksAndMessages(null)
            handler = Handler()
            handler?.postDelayed({
                when (it) {
                    false -> navigateToOnBoard()
                    true -> navigateToMainActivity()
                }
            }, navigationDelayInMilliseconds)
        })
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToOnBoard() {
        val intent = Intent(this, OnBoardingNavActivity::class.java)
        startActivity(intent)
        finish()
    }
}
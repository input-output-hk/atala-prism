package io.iohk.cvp.neo.ui.launch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoActivityLaunchBinding
import io.iohk.cvp.neo.ui.onboarding.OnBoardingNavActivity
import io.iohk.cvp.views.activities.MainActivity
import io.iohk.cvp.views.utils.ForegroundBackgroundListener

class LaunchActivity : AppCompatActivity() {

    private val viewModel: LaunchViewModel by viewModels { LaunchViewModelFactory }

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
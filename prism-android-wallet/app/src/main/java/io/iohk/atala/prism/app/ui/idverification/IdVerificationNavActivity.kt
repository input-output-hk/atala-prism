package io.iohk.atala.prism.app.ui.idverification

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.android.support.DaggerAppCompatActivity
import io.iohk.cvp.R
import io.iohk.cvp.databinding.ActivityNavIdVerificationBinding

/**
 * [IdVerificationNavActivity]  this class encapsulates all the navigation of
 * the "id verification" process, which is defined in the navigation
 * resource "res/navigation/id_verification.xml"
 * */
class IdVerificationNavActivity : DaggerAppCompatActivity() {

    private lateinit var binding: ActivityNavIdVerificationBinding

    private val navController: NavController by lazy {
        findNavController(R.id.id_verification_nav_host_fragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_nav_id_verification)
        binding.lifecycleOwner = this
        setupActionBarWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp()
}

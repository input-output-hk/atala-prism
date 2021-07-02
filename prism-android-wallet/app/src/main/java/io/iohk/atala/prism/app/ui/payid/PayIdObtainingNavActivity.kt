package io.iohk.atala.prism.app.ui.payid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import io.iohk.cvp.R
import io.iohk.cvp.databinding.ActivityNavPayIdObtainingBinding

class PayIdObtainingNavActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNavPayIdObtainingBinding

    private val navController: NavController by lazy {
        findNavController(R.id.payid_obtaining_nav_host_fragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_nav_pay_id_obtaining)
        binding.lifecycleOwner = this
        setupActionBarWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp()
}

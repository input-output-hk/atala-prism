package io.iohk.atala.prism.app.neo.ui.onboarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoActivityNavOnBoardingBinding

/**
 *  [OnBoardingNavActivity] this class encapsulates all the navigation
 *  of the "on boarding" process, which is defined in the navigation
 *  resource "res/navigation/on_boarding.xml"
 * */
class OnBoardingNavActivity : AppCompatActivity() {

    private lateinit var binding: NeoActivityNavOnBoardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.neo_activity_nav_on_boarding)
        binding.lifecycleOwner = this
    }
}

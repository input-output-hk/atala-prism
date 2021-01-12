package io.iohk.atala.prism.app.neo.ui.onboarding.restoreaccountsuccess

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.findNavController
import io.iohk.cvp.databinding.NeoFragmentRestoreAccountSuccessBinding
import io.iohk.cvp.R

class RestoreAccountSuccessFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: NeoFragmentRestoreAccountSuccessBinding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_restore_account_success, container, false)
        binding.nextBtn.setOnClickListener {
            // Navigate to Main Activity
            val extras = ActivityNavigator.Extras.Builder()
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .build()
            val action = RestoreAccountSuccessFragmentDirections.actionRestoreAccountSuccessFragmentToMainActivity()
            findNavController().navigate(action, extras)
        }
        return binding.root
    }
}
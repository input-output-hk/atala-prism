package io.iohk.atala.prism.app.neo.ui.onboarding.accountcreated

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.findNavController
import io.iohk.cvp.databinding.NeoFragmentAccountCreatedBinding
import io.iohk.cvp.R

class AccountCreatedFragment : Fragment() {

    private lateinit var binding: NeoFragmentAccountCreatedBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_account_created, container, false)
        binding.lifecycleOwner = this
        binding.continueButton.setOnClickListener {
            val extras = ActivityNavigator.Extras.Builder()
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .build()
            val action = AccountCreatedFragmentDirections.actionAccountCreatedFragmentToMainActivity()
            findNavController().navigate(action, extras)
        }
        return binding.root
    }
}
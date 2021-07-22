package io.iohk.atala.prism.app.ui.idverification

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.findNavController
import io.iohk.cvp.databinding.FragmentIdVerificationSuccessBinding

class IdVerificationSuccessFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // custom toolbar back button behavior
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentIdVerificationSuccessBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.closeButton.setOnClickListener {
            goToMainActivity()
        }
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // Android back button behavior
            goToMainActivity()
        }
        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // custom toolbar back button behavior
            goToMainActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun goToMainActivity() {
        val extras = ActivityNavigator.Extras.Builder()
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .build()
        val action = IdVerificationSuccessFragmentDirections.actionIdVerificationSuccessFragmentToMainActivity()
        findNavController().navigate(action, extras)
    }
}

package io.iohk.atala.prism.app.ui.idverification.step2

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_FACE_CAPTURE_OPTIONS
import com.acuant.acuantfacecapture.FaceCaptureActivity
import com.acuant.acuantfacecapture.model.FaceCaptureOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.extensions.buildActivityResultLauncher
import io.iohk.atala.prism.app.neo.common.extensions.decodeBitmapFromUrl
import io.iohk.atala.prism.app.neo.common.extensions.hideBlockUILoading
import io.iohk.atala.prism.app.neo.common.extensions.showBlockUILoading
import io.iohk.atala.prism.app.neo.common.extensions.toast
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentIdSelfieBinding
import javax.inject.Inject

class IdSelfieFragment : DaggerFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: IdSelfieViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(IdSelfieViewModel::class.java)
    }

    private val acuantFaceCaptureLauncher = buildActivityResultLauncher { activityResult ->
        when (activityResult.resultCode) {
            FaceCaptureActivity.RESPONSE_SUCCESS_CODE -> {
                activityResult.data?.getStringExtra(FaceCaptureActivity.OUTPUT_URL)?.let { photoUrl ->
                    decodeBitmapFromUrl(photoUrl) { bitmap ->
                        bitmap?.let {
                            viewModel.processSelfie(it)
                        } ?: toast(R.string.commons_error_decoding_a_image_file)
                    }
                }
            }
            FaceCaptureActivity.RESPONSE_CANCEL_CODE -> { /* user canceling */ }
            else -> {
                // handle error during capture
                showFaceMatchingRetryDialog(R.string.fragment_id_selfie_error_face_matching_process)
            }
        }
    }

    private lateinit var binding: FragmentIdSelfieBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentIdSelfieBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.takeAPictureButtom.setOnClickListener {
            startFaceCaptureActivity()
        }
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                requireActivity().showBlockUILoading()
            } else {
                requireActivity().hideBlockUILoading()
            }
        }

        viewModel.faceMatchingFailed.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    showFaceMatchingRetryDialog(R.string.fragment_id_selfie_error_face_matching_process)
                }
            }
        )

        viewModel.faceMatchingSuccess.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    findNavController().navigate(R.id.action_idSelfieFragment_to_idDataConfirmationFragment)
                }
            }
        )
    }

    private fun startFaceCaptureActivity() {
        val cameraIntent = Intent(requireContext(), FaceCaptureActivity::class.java)
        cameraIntent.putExtra(ACUANT_EXTRA_FACE_CAPTURE_OPTIONS, FaceCaptureOptions())
        acuantFaceCaptureLauncher.launch(cameraIntent)
    }

    private fun showFaceMatchingRetryDialog(messageResourceId: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(messageResourceId)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel) { _: DialogInterface, _: Int ->
                findNavController().popBackStack()
            }
            .setPositiveButton(R.string.retry) { _: DialogInterface, _: Int ->
                startFaceCaptureActivity()
            }
            .show()
    }
}

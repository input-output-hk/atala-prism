package io.iohk.atala.prism.app.ui.idverification.step1

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.animation.doOnEnd
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_CAMERA_OPTIONS
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IMAGE_URL
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_PDF417_BARCODE
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.extensions.buildActivityResultLauncher
import io.iohk.atala.prism.app.neo.common.extensions.decodeBitmapFromUrl
import io.iohk.atala.prism.app.neo.common.extensions.hideBlockUILoading
import io.iohk.atala.prism.app.neo.common.extensions.showBlockUILoading
import io.iohk.atala.prism.app.neo.common.extensions.showErrorDialog
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.atala.prism.app.neo.common.extensions.toast
import io.iohk.atala.prism.app.neo.data.KycRepository
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentDocumentScanBinding
import javax.inject.Inject

class DocumentScanFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: DocumentScanViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(DocumentScanViewModel::class.java)
    }

    private lateinit var binding: FragmentDocumentScanBinding

    private val args: DocumentScanFragmentArgs by navArgs()

    private val idType: IdTypeSelectionViewModel.IdType by lazy {
        IdTypeSelectionViewModel.IdType.valueOf(args.documentType)
    }

    private val acuantCameraLauncher = buildActivityResultLauncher { activityResult ->
        if (activityResult.resultCode == AcuantCameraActivity.RESULT_SUCCESS_CODE) {
            activityResult.data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)?.let { photoUrl ->
                val barcodeString = activityResult.data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
                decodeBitmapFromUrl(photoUrl) { bitmap ->
                    bitmap?.let {
                        viewModel.processDocument(it, barcodeString)
                    } ?: toast(R.string.commons_error_decoding_a_image_file)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleBackButton()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_document_scan, container, false)
        binding.lifecycleOwner = this
        // Get the name of the document (ID, Passport or Driver's License) and send it to the layout
        binding.documentType = when (idType) {
            IdTypeSelectionViewModel.IdType.NationalId -> getString(R.string.document_scan_id_type_national_id)
            IdTypeSelectionViewModel.IdType.DriverLicense -> getString(R.string.document_scan_id_type_driver_license)
            IdTypeSelectionViewModel.IdType.Passport -> getString(R.string.document_scan_id_type_passport)
        }
        binding.takeAPictureButtom.setOnClickListener {
            takeAPicture()
        }
        supportActionBar?.show()
        setObservers()
        return binding.root
    }

    private fun takeAPicture() {
        val intent = Intent(requireContext(), AcuantCameraActivity::class.java)
        val extra = AcuantCameraOptions.DocumentCameraOptionsBuilder().build()
        intent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS, extra)
        acuantCameraLauncher.launch(intent)
    }

    private fun setObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                requireActivity().showBlockUILoading()
            } else {
                requireActivity().hideBlockUILoading()
            }
        }

        viewModel.showError.observe(
            viewLifecycleOwner,
            EventWrapperObserver { error ->
                val errorMsg = when (error) {
                    is KycRepository.KycRepositoryError.AcuantError -> error.message
                    is KycRepository.KycRepositoryError.DocumentPhotoNotFound -> getString(R.string.document_scan_error_document_photo_not_found)
                    else -> getString(R.string.generic_error_message)
                }
                requireActivity().showErrorDialog(errorMsg)
            }
        )

        viewModel.frontDocumentLoaded.observe(viewLifecycleOwner) { frontDocumentResult ->
            if (frontDocumentResult) {
                animateNext()
            } else {
                animateBack()
            }
        }

        viewModel.shouldGoToNextStep.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    findNavController().navigate(R.id.action_documentScanFragment_to_idSelfieFragment)
                }
            }
        )
    }

    /*
    * Handle a custom "back behavior"
    * */

    private fun handleBackButton() {
        setHasOptionsMenu(true)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            back()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            back()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun back() {
        if (viewModel.frontDocumentLoaded.value!!) {
            viewModel.resetProcess()
        } else {
            findNavController().popBackStack()
        }
    }

    /*
    * Handle Transition between sides instructions (Animations)
    * */
    private val animatorSet: AnimatorSet by lazy {
        // Set of Animations for card images
        val cardsSet = AnimatorSet()
        val frontAnimatorSet = AnimatorInflater.loadAnimator(requireContext(), R.animator.flip_rotation_y_0_90) as AnimatorSet
        frontAnimatorSet.setTarget(binding.frontImage)
        val backAnimatorSet = AnimatorInflater.loadAnimator(requireContext(), R.animator.flip_rotation_y_minus_90_0) as AnimatorSet
        backAnimatorSet.setTarget(binding.backImage)
        cardsSet.playSequentially(frontAnimatorSet, backAnimatorSet)
        // Set of animations for instructions texts
        val instructionsSet = AnimatorSet()
        val frontInstructionsSet = AnimatorInflater.loadAnimator(requireContext(), R.animator.alpha_100_0) as AnimatorSet
        frontInstructionsSet.setTarget(binding.frontInstructions)
        val backInstructionsSet = AnimatorInflater.loadAnimator(requireContext(), R.animator.alpha_0_100) as AnimatorSet
        backInstructionsSet.setTarget(binding.backInstructions)
        instructionsSet.playSequentially(frontInstructionsSet, backInstructionsSet)
        // Main Animation Set
        val set = AnimatorSet()
        set.playTogether(cardsSet, instructionsSet)
        set.doOnEnd {
            isInFrontCaptureStep = binding.backInstructions.alpha == 0f
        }
        return@lazy set
    }

    private var isInFrontCaptureStep: Boolean = true

    private fun animateNext() {
        if (isInFrontCaptureStep) {
            animatorSet.start()
        }
    }

    private fun animateBack() {
        if (!isInFrontCaptureStep) {
            animatorSet.reverse()
        }
    }
}

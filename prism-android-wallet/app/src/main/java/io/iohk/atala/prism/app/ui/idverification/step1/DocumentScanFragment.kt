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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_CAMERA_OPTIONS
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IMAGE_URL
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.extensions.buildActivityResultLauncher
import io.iohk.atala.prism.app.neo.common.extensions.documentIssueDate
import io.iohk.atala.prism.app.neo.common.extensions.documentNumber
import io.iohk.atala.prism.app.neo.common.extensions.getGender
import io.iohk.atala.prism.app.neo.common.extensions.hideBlockUILoading
import io.iohk.atala.prism.app.neo.common.extensions.showBlockUILoading
import io.iohk.atala.prism.app.neo.common.extensions.showErrorDialog
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
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
            activityResult.data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)?.let {
                viewModel.processDocument(it)
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
            EventWrapperObserver {
                if (it.first) {
                    val errorMsg = it.second?.errorDescription
                        ?: requireActivity().getString(R.string.generic_error_message)
                    requireActivity().showErrorDialog(errorMsg)
                }
            }
        )

        viewModel.frontDocumentResult.observe(viewLifecycleOwner) { frontDocumentResult ->
            if (frontDocumentResult != null) {
                animateNext()
            }
        }

        viewModel.shouldGoNextStep.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                // TODO This information has to be managed by a data repository, in the following tickets that will be added with an appropriate data model
                val direction = DocumentScanFragmentDirections.actionDocumentScanFragmentToIdDataConfirmationFragment(
                    it.biographic.fullName,
                    it.getGender(),
                    it.classification.type.countryCode,
                    it.documentNumber() ?: "",
                    it.biographic.birthDate,
                    it.documentIssueDate() ?: "",
                    it.biographic.expirationDate
                )
                findNavController().navigate(direction)
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
        if (viewModel.frontDocumentResult.value != null) {
            animateBack()
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
        return@lazy set
    }

    private fun animateNext() = animatorSet.start()

    private fun animateBack() = animatorSet.reverse()
}

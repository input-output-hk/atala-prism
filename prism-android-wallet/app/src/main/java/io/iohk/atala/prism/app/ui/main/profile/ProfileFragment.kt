package io.iohk.atala.prism.app.ui.main.profile

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.os.Environment
import android.view.*
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.IntentUtils
import io.iohk.atala.prism.app.neo.common.extensions.*
import io.iohk.atala.prism.app.utils.PermissionUtils
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentProfileBinding
import java.io.File
import javax.inject.Inject

class ProfileFragment : DaggerFragment() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel:ProfileViewModel by lazy {
        ViewModelProviders.of(this, factory).get(ProfileViewModel::class.java)
    }

    lateinit var binding: FragmentProfileBinding

    private val temporalPhotoFile: File by lazy {
        createTempFile("PROFILE_TEMP_PHOTO", ".jpg", requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    }

    private val cameraActivityLauncher = buildActivityResultLauncher { activityResult ->
        // Handle when take a photo from the camera app
        if (activityResult.resultCode == Activity.RESULT_OK && temporalPhotoFile.exists()) {
            viewModel.handleProfilePhotoUri(temporalPhotoFile.toUri(),requireContext())
        }
    }

    private val galleryActivityLauncher = buildActivityResultLauncher { activityResult ->
        // Handle when selected a photo from the library
        if (activityResult.resultCode == Activity.RESULT_OK) {
            activityResult?.data?.data?.let { imageUri ->
                viewModel.handleProfilePhotoUri(imageUri,requireContext())
            }
        }
    }

    private val cameraPermissionLauncher = buildRequestPermissionLauncher { permissionGuaranteed ->
        if(permissionGuaranteed) showEditProfilePictureMenu()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        configureEditPicButton()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.loadPreferences()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) = inflater.inflate(R.menu.profile_menu, menu)

    override fun onPrepareOptionsMenu(menu: Menu) {
        val isEditMode:Boolean =  viewModel.editMode.value == true
        menu.findItem(R.id.action_edit_profile).isVisible = !isEditMode
        menu.findItem(R.id.action_save_profile).isVisible = isEditMode
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_save_profile -> {
                viewModel.savePreferences()
                true
            }
            R.id.action_edit_profile -> {
                viewModel.setEditMode()
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setObservers() {
        viewModel.editMode.observe(viewLifecycleOwner) { editMode ->
            requireActivity().invalidateOptionsMenu()
            supportActionBar?.title = if(editMode) getString(R.string.edit_profile) else getString(R.string.profile)
        }
        viewModel.showLoading.observe(viewLifecycleOwner,EventWrapperObserver{
            if (it) requireActivity().showBlockUILoading()
            else requireActivity().hideBlockUILoading()
        })
    }

    private fun configureEditPicButton() {
        binding.editPicButton.setOnClickListener {
            if(PermissionUtils.checkIfAlreadyHavePermission(requireContext(), Manifest.permission.CAMERA)){
                showEditProfilePictureMenu()
            }else{
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showEditProfilePictureMenu() {
        val options: List<Int> = listOf(
                R.string.profile_pic_menu_camera,
                R.string.profile_pic_menu_library,
                R.string.profile_pic_menu_remove
        )
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setItems(options) { _: DialogInterface?, which: Int ->
            when (options[which]) {
                R.string.profile_pic_menu_camera -> cameraActivityLauncher.launch(IntentUtils.intentCamera(requireContext(),temporalPhotoFile))
                R.string.profile_pic_menu_library -> galleryActivityLauncher.launch(IntentUtils.intentGallery())
                R.string.profile_pic_menu_remove -> viewModel.clearProfileImage()
            }
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }
}
package io.iohk.atala.prism.app.ui.main.profile

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.*
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.iohk.atala.prism.app.neo.common.FileUtils
import io.iohk.atala.prism.app.neo.common.extensions.*
import io.iohk.atala.prism.app.ui.CvpFragment
import io.iohk.atala.prism.app.ui.utils.AppBarConfigurator
import io.iohk.atala.prism.app.ui.utils.RootAppBar
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class ProfileFragment : CvpFragment<ProfileViewModel>() {

    companion object {
        private const val REQUEST_CAMERA = 1
        private const val REQUEST_LIBRARY = 2
        private const val PERMISSION_REQUEST_CAMERA = 3
        private const val MAX_PROFILE_PHOTO_PX_SIZE = 800
    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    lateinit var binding: FragmentProfileBinding

    private val temporalPhotoFile: File by lazy {
        createTempFile("PROFILE_TEMP_PHOTO", ".jpg", requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, viewId, container, false)
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

    override fun getViewModel(): ProfileViewModel {
        return ViewModelProvider(this, factory).get(ProfileViewModel::class.java)
    }

    override fun getAppBarConfigurator(): AppBarConfigurator {
        // TODO this has to be refactored when we remove the inheritance from [CvpFragment]
        return RootAppBar(if (viewModel.editMode.value == true) R.string.edit_profile else R.string.profile)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        // TODO this has to be refactored when we remove the inheritance from [CvpFragment]
        menu.findItem(R.id.action_edit_profile).isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // TODO this has to be refactored when we remove the inheritance from [CvpFragment]
        if (item.itemId == R.id.action_edit_profile) {
            val isInEditMode = viewModel.editMode.value == true
            if (isInEditMode) {
                viewModel.savePreferences()
                item.icon = requireActivity().getDrawable(R.drawable.ic_edit)
            } else {
                viewModel.setEditMode()
                item.icon = requireActivity().getDrawable(R.drawable.ic_check)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getViewId(): Int {
        return R.layout.fragment_profile
    }

    private fun setObservers() {
        viewModel.editMode.observe(viewLifecycleOwner) {
            setActionBar()
        }
        viewModel.showLoading.observe(viewLifecycleOwner) {
            if (it)
                requireActivity().showBlockUILoading()
            else
                requireActivity().hideBlockUILoading()
        }
    }

    private fun configureEditPicButton() {
        binding.editPicButton.setOnClickListener {
            if (cameraPermissionExists(PERMISSION_REQUEST_CAMERA, requestIfDoesNotExist = true)) {
                showEditProfilePictureMenu()
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
        builder.setItems(options, DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
            when (options[which]) {
                R.string.profile_pic_menu_camera -> {
                    startCameraActivity(REQUEST_CAMERA, temporalPhotoFile)
                }
                R.string.profile_pic_menu_library -> startGalleryActivity(REQUEST_LIBRARY)
                R.string.profile_pic_menu_remove -> viewModel.clearProfileImage()
            }
        }).setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            // Handle when take a photo from the camera app
            if (temporalPhotoFile.exists()) {
                handleProfilePhotoUri(temporalPhotoFile.toUri())
            }
        } else if (requestCode == REQUEST_LIBRARY && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { imageUri ->
                // Handle when selected a photo from the library
                handleProfilePhotoUri(imageUri)
            }
        }
    }

    private fun handleProfilePhotoUri(imageUri: Uri) {
        CoroutineScope(Dispatchers.Main).launch {
            requireActivity().showBlockUILoading()
            FileUtils.decodeBitmapFromUri(requireContext(), imageUri, MAX_PROFILE_PHOTO_PX_SIZE)?.let {
                viewModel.setProfileImage(it)
            }
            requireActivity().hideBlockUILoading()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CAMERA &&
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showEditProfilePictureMenu()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
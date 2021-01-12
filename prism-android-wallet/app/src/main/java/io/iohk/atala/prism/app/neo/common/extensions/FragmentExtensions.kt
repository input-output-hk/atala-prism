package io.iohk.atala.prism.app.neo.common.extensions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import io.iohk.atala.prism.app.utils.PermissionUtils
import java.io.File

fun Fragment.startCameraActivity(requestCode: Int) {
    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    startActivityForResult(takePictureIntent, requestCode)
}

fun Fragment.startCameraActivity(requestCode: Int, photoFile: File) {
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
        takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "io.iohk.cvp.fileprovider",
                    photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, requestCode)
        }
    }
}

fun Fragment.startGalleryActivity(requestCode: Int) {
    val intent = Intent()
    intent.type = "image/*"
    intent.action = Intent.ACTION_GET_CONTENT
    startActivityForResult(Intent.createChooser(intent, null), requestCode)
}

fun Fragment.cameraPermissionExists(requestCode: Int, requestIfDoesNotExist: Boolean): Boolean {
    return if (!PermissionUtils.checkIfAlreadyHavePermission(requireContext(), Manifest.permission.CAMERA)) {
        if (requestIfDoesNotExist) {
            PermissionUtils.requestForSpecificPermission(this, requestCode, Manifest.permission.CAMERA)
        }
        false
    } else true
}
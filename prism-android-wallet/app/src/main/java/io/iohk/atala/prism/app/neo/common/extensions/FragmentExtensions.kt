package io.iohk.atala.prism.app.neo.common.extensions

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.iohk.atala.prism.app.neo.common.FileUtils
import kotlinx.coroutines.launch

val Fragment.supportActionBar: ActionBar?
    get() = (requireActivity() as? AppCompatActivity)?.supportActionBar

fun Fragment.buildActivityResultLauncher(callBack: ActivityResultCallback<ActivityResult>): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult(), callBack)
}

fun Fragment.buildRequestPermissionLauncher(callBack: ActivityResultCallback<Boolean>): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission(), callBack)
}

fun Fragment.decodeBitmapFromUrl(url: String, maxSize: Int? = null, action: (bitmap: Bitmap?) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        action(FileUtils.decodeBitmapFromUrl(requireContext(), url, maxSize))
    }
}

fun Fragment.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), resId, duration).show()
}

fun Fragment.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), message, duration).show()
}

/*
* This is a key to handle a standard result for FragmentResult
* */
val Fragment.KEY_RESULT: String
    get() = "KET_RESULT"

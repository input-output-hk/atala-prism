package io.iohk.atala.prism.app.neo.common.extensions

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment


val Fragment.supportActionBar:ActionBar?
    get() = (requireActivity() as? AppCompatActivity)?.supportActionBar

fun Fragment.buildActivityResultLauncher(callBack:ActivityResultCallback<ActivityResult>):ActivityResultLauncher<Intent>{
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult(),callBack)
}

fun Fragment.buildRequestPermissionLauncher(callBack:ActivityResultCallback<Boolean>):ActivityResultLauncher<String>{
    return registerForActivityResult(ActivityResultContracts.RequestPermission(),callBack)
}

/*
* This is a key to handle a standard result for FragmentResult
* */
val Fragment.KEY_RESULT: String
    get() = "KET_RESULT"
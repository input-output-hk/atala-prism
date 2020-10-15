package io.iohk.cvp.neo.common.extensions

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.iohk.cvp.neo.ui.commondialogs.LoadingDialog
import io.iohk.cvp.utils.IntentDataConstants
import io.iohk.cvp.views.fragments.PopUpFragment
import io.iohk.cvp.R

/*
* Show an error dialog in an [FragmentActivity]
* */

fun FragmentActivity.showErrorDialog(errorMsgResource: Int) {
    showErrorDialog(getString(errorMsgResource))
}

fun FragmentActivity.showErrorDialog(errorMsg: String?) {
    val bundle = Bundle()
    bundle.putBoolean(IntentDataConstants.POP_UP_IS_ERROR, errorMsg != null)
    bundle.putString(IntentDataConstants.ERROR_MSG_DESCRIPTION_KEY, errorMsg)
    val fragment = PopUpFragment()
    fragment.arguments = bundle
    fragment.show(supportFragmentManager, null)
}

/*
* Handle a loading dialog that locks the UI of an [FragmentActivity]
* */

fun FragmentActivity.showBlockUILoading() {
    if (getBlockUIDialog()?.dialog?.isShowing == true) {
        return
    }
    val loadingDialog = LoadingDialog()
    loadingDialog.show(supportFragmentManager, null)
    window.decorView.setTag(R.string.TAG_LOADING_UI_BLOCK, loadingDialog)
}

fun FragmentActivity.hideBlockUILoading() {
    if (getBlockUIDialog()?.dialog?.isShowing == true) {
        getBlockUIDialog()?.dismiss()
        window.decorView.setTag(R.string.TAG_LOADING_UI_BLOCK, null)
    }
}

private fun FragmentActivity.getBlockUIDialog(): DialogFragment? {
    return window.decorView.getTag(R.string.TAG_LOADING_UI_BLOCK) as? DialogFragment
}
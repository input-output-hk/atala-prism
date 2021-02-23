package io.iohk.atala.prism.app.neo.common.extensions

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.iohk.atala.prism.app.neo.ui.commondialogs.LoadingDialog
import io.iohk.atala.prism.app.ui.commondialogs.PopUpFragment
import io.iohk.atala.prism.app.utils.IntentDataConstants
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
    getBlockUIDialog()?.dismiss()
    window.decorView.setTag(R.string.TAG_LOADING_UI_BLOCK, null)
}

private fun FragmentActivity.getBlockUIDialog(): DialogFragment? {
    return window.decorView.getTag(R.string.TAG_LOADING_UI_BLOCK) as? DialogFragment
}

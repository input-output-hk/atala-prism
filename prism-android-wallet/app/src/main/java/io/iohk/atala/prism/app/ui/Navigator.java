package io.iohk.atala.prism.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;


import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.braintreepayments.api.dropin.DropInRequest;

import java.util.List;

import io.iohk.cvp.R;
import io.iohk.atala.prism.app.utils.ActivitiesRequestCodes;
import io.iohk.atala.prism.app.utils.IntentDataConstants;
import io.iohk.atala.prism.app.ui.commondialogs.PopUpFragment;

import static io.iohk.atala.prism.app.utils.ActivitiesRequestCodes.BRAINTREE_REQUEST_ACTIVITY;
import static io.iohk.atala.prism.app.ui.main.MainActivity.MAIN_FRAGMENT_TAG;

/*
 * @TODO this needs to be removed when the navigation components are implemented
 * */
public class Navigator {

    public void showWebView(Activity from) {
        startNewActivity(from, WebViewActivity.class, null);
    }

    public void showQrScanner(CvpFragment from) {
        Intent intent = new Intent(from.requireContext(),
                QrCodeScannerActivity.class);
        intent.putExtra(IntentDataConstants.QR_SCANNER_MODE_KEY,
                IntentDataConstants.QR_SCANNER_MODE_QR_CODE);
        from.startActivityForResult(intent, ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY);
    }

    public void showPayment(Activity from, String tokenizationKey) {
        DropInRequest dropInRequest = new DropInRequest().clientToken(tokenizationKey);
        Intent intent = dropInRequest.getIntent(from);
        from.startActivityForResult(intent, BRAINTREE_REQUEST_ACTIVITY);
    }

    private void startNewActivity(Activity from, Class activityClass,
                                  List<Integer> flags) {
        Intent intent = getIntent(from, activityClass, flags);
        from.startActivity(intent);
    }

    private Intent getIntent(Activity from, Class activityClass, List<Integer> flags) {
        Intent intent = new Intent(from.getApplicationContext(), activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (flags != null) {
            flags.forEach(intent::addFlags);
        }

        return intent;
    }

    public void showPopUp(FragmentManager fragmentManager, String errorMessage) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(IntentDataConstants.POP_UP_IS_ERROR, errorMessage != null);
        bundle.putString(IntentDataConstants.ERROR_MSG_DESCRIPTION_KEY, errorMessage);

        PopUpFragment fragment = new PopUpFragment();
        fragment.setArguments(bundle);

        String tag = "ERROR_POP_UP";

        if (fragmentManager.findFragmentByTag(tag) == null) {
            fragmentManager
                    .beginTransaction().add(fragment, tag)
                    .show(fragment).commit();
        }
    }

    public void showAppPermissionSettings(Activity from) {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + from.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        from.startActivity(intent);
    }

    public void showFragment(FragmentManager supportFragmentManager, CvpFragment cvpFragment,
                             String tag, String transactionTag) {
        FragmentTransaction ft = supportFragmentManager.beginTransaction();
        ft.replace(R.id.fragment_layout, cvpFragment, tag);
        ft.addToBackStack(transactionTag);
        ft.commit();
    }

    public void showFragmentOnTop(FragmentManager supportFragmentManager, CvpFragment cvpFragment) {
        showFragment(supportFragmentManager, cvpFragment, MAIN_FRAGMENT_TAG, null);
    }

    public void showFragmentOnTopOfMenu(FragmentManager supportFragmentManager,
                                        CvpFragment cvpFragment) {
        FragmentTransaction ft = supportFragmentManager.beginTransaction();
        ft.replace(R.id.fragment_layout_over_menu, cvpFragment, MAIN_FRAGMENT_TAG);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void showFragmentOnTopOfMenuNoBackstack(FragmentManager supportFragmentManager,
                                                   CvpFragment cvpFragment) {
        FragmentTransaction ft = supportFragmentManager.beginTransaction();
        ft.replace(R.id.fragment_layout_over_menu, cvpFragment, MAIN_FRAGMENT_TAG);
        ft.commit();
    }

    public void showDialogFragment(FragmentManager supportFragmentManager,
                                   CvpDialogFragment cvpFragment, String tag) {

        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager
                    .beginTransaction().add(cvpFragment, tag)
                    .show(cvpFragment)
                    .commit();
        }
    }

    public void showUnlockScreen(Context ctx) {
        Intent myIntent = new Intent(ctx, UnlockActivity.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(myIntent);
    }
}

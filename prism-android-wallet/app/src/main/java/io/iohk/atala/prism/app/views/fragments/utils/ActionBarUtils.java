package io.iohk.atala.prism.app.views.fragments.utils;

import android.Manifest;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;

import java.util.Objects;

import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.utils.ActivitiesRequestCodes;
import io.iohk.atala.prism.app.utils.PermissionUtils;
import io.iohk.atala.prism.app.views.Navigator;
import io.iohk.atala.prism.app.views.fragments.AddQrCodeDialogFragment;
import io.iohk.atala.prism.app.views.fragments.CvpFragment;

public class ActionBarUtils {

    private static final String ADD_QR_CODE_DIALOG_FRAGMENT = "addQrCodeDialogFragment";

    public static void setTextColor(ActionBar actBar, int color) {
        String title = Objects.requireNonNull(actBar.getTitle()).toString();
        Spannable spannablerTitle = new SpannableString(title);
        spannablerTitle.setSpan(new ForegroundColorSpan(color), 0, spannablerTitle.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        actBar.setTitle(spannablerTitle);
    }

    public static void setupMenu(Menu menu) {
        setupMenuQrOptional(menu, true);
    }

    public static void setupMenuQrOptional(Menu menu, boolean showQrScanButton) {
        if (showQrScanButton) {
            MenuItem actionNewConnection = menu.findItem(R.id.action_new_connection);
            actionNewConnection.setVisible(true);
        }
        if (BuildConfig.DEBUG) {
            MenuItem addNewBtn = menu.findItem(R.id.addNewBtn);
            addNewBtn.setVisible(true);
        }

    }

    public static boolean menuItemClicked(Navigator navigator, MenuItem item, CvpFragment fragment) {
        if (item.getItemId() == R.id.action_new_connection) {
            if (!PermissionUtils
                    .checkIfAlreadyHavePermission(fragment.getActivity().getApplicationContext(),
                            Manifest.permission.CAMERA)) {
                PermissionUtils.requestForSpecificPermission(fragment, ActivitiesRequestCodes
                        .QR_SCANNER_REQUEST_PERMISSION, Manifest.permission.CAMERA);
            } else {
                navigator.showQrScanner(fragment);
            }

            return true;
        }
        if (item.getItemId() == R.id.addNewBtn) {
            AddQrCodeDialogFragment addQrCodeDialogFragment = AddQrCodeDialogFragment.newInstance();
            addQrCodeDialogFragment.setTargetFragment(fragment, ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY);
            navigator.showDialogFragment(fragment.requireActivity().getSupportFragmentManager(),
                    addQrCodeDialogFragment, ADD_QR_CODE_DIALOG_FRAGMENT);
            return true;
        }
        return false;
    }

}

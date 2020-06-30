package io.iohk.cvp.views.fragments.utils;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;

import java.util.Objects;

import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.ActivitiesRequestCodes;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.AddQrCodeDialogFragment;
import io.iohk.cvp.views.fragments.CvpFragment;

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
      navigator.showQrScanner(fragment);
      return true;
    }
    if (item.getItemId() == R.id.addNewBtn) {
      AddQrCodeDialogFragment addQrCodeDialogFragment = AddQrCodeDialogFragment.newInstance();
      addQrCodeDialogFragment.setTargetFragment(fragment, ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY);
      navigator.showDialogFragment(Objects.requireNonNull(fragment.getFragmentManager()),
              addQrCodeDialogFragment, ADD_QR_CODE_DIALOG_FRAGMENT);
      return true;
    }
    return false;
  }

}

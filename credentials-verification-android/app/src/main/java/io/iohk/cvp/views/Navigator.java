package io.iohk.cvp.views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.fragment.app.FragmentActivity;
import io.iohk.cvp.utils.ActivitiesRequestCodes;
import io.iohk.cvp.utils.IntentDataConstants;
import io.iohk.cvp.views.activities.ConnectionActivity;
import io.iohk.cvp.views.activities.CvpActivity;
import io.iohk.cvp.views.activities.QrCodeScanner;
import io.iohk.cvp.views.fragments.PopUpFragment;

public class Navigator {

  //Activities
  public void showConnections(CvpActivity from) {
    Intent intent = new Intent(from.getApplicationContext(), ConnectionActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    from.startActivity(intent);
  }

  public void showQrScanner(CvpActivity from) {
    Intent intent = new Intent(from.getApplicationContext(), QrCodeScanner.class);
    intent.putExtra(IntentDataConstants.QR_SCANNER_MODE_KEY,
        IntentDataConstants.QR_SCANNER_MODE_QR_CODE);
    from.startActivityForResult(intent, ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY);
  }

  public void showPermissionDeniedPopUp(CvpActivity from) {
    Bundle bundle = new Bundle();
    PopUpFragment fragment = new PopUpFragment();
    fragment.setArguments(bundle);
    from.getSupportFragmentManager()
        .beginTransaction().add(fragment, "PERMISSION_DENIED_POPUP")
        .show(fragment).commit();
  }

  public void showAppPermissionSettings(FragmentActivity from) {
    final Intent intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.addCategory(Intent.CATEGORY_DEFAULT);
    intent.setData(Uri.parse("package:" + from.getPackageName()));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    from.startActivity(intent);
  }

}

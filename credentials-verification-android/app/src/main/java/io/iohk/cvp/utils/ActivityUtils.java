package io.iohk.cvp.utils;

import android.app.Activity;
import android.content.Intent;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.AcceptConnectionDialogFragment;
import io.iohk.cvp.views.fragments.CvpFragment;
import java.util.Objects;

public class ActivityUtils {

  public static void onQrcodeResult(int requestCode, int resultCode,
      MainActivity activity,
      ConnectionsActivityViewModel viewModel, Intent data, CvpFragment fragment) {
    if (requestCode == ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY
        && resultCode == Activity.RESULT_OK) {
      String token = data.getStringExtra(IntentDataConstants.QR_RESULT);

      viewModel.getConnectionTokenInfo(token)
          .observe(fragment, issuerInfo ->
              fragment.getNavigator().showDialogFragment(
                  Objects.requireNonNull(activity).getSupportFragmentManager(),
                  AcceptConnectionDialogFragment
                      .newInstance(token, issuerInfo),
                  "ACCEPT_CONNECTION_DIALOG_FRAGMENT")
          );
    }
  }

}

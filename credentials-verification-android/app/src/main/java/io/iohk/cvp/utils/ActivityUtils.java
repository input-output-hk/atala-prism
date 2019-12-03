package io.iohk.cvp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import io.iohk.cvp.core.exception.CryptoException;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.CvpFragment;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import java.security.spec.InvalidKeySpecException;

public class ActivityUtils {

  public static void onQrcodeResult(int requestCode, int resultCode, Context context,
      MainActivity activity,
      ConnectionsActivityViewModel viewModel, Intent data, CvpFragment fragment) {
    if (requestCode == ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY
        && resultCode == Activity.RESULT_OK) {
      String token = data.getStringExtra(IntentDataConstants.QR_RESULT);

      viewModel.getConnectionTokenInfo(token)
          .observe(fragment, issuerInfo -> {
            // TODO show issuer data to confirm connection and after confirmation call
            Preferences prefs = new Preferences(context);
            try {
              viewModel
                  .addConnectionFromToken(token,
                      CryptoUtils.getPublicKey(prefs))
                  .observe(fragment, connectionInfo -> {
                    //TODO should we show new connection info before switching to connections list?
                    // if we decide not to do so, addConnectionFromToken method should be moved from view model
                    prefs.saveUserId(connectionInfo.getUserId());
                    activity.onNavigation(BottomAppBarOption.CONNECTIONS);
                  });
            } catch (SharedPrefencesDataNotFoundException | InvalidKeySpecException | CryptoException e) {
              e.printStackTrace();
            }
          });
    }
  }

}

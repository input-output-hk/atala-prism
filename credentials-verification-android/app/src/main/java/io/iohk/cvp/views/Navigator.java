package io.iohk.cvp.views;

import android.content.Intent;
import io.iohk.cvp.utils.ActivitiesRequestCodes;
import io.iohk.cvp.utils.IntentDataConstants;
import io.iohk.cvp.views.activities.ConnectionActivity;
import io.iohk.cvp.views.activities.CvpActivity;
import io.iohk.cvp.views.activities.QrCodeScanner;

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

}

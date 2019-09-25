package io.iohk.cvp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtils {

  public static boolean checkIfAlreadyHavePermission(Context context, String permission) {
    int result = ContextCompat.checkSelfPermission(context, permission);
    return result == PackageManager.PERMISSION_GRANTED;
  }

  public static void requestForSpecificPermission(Activity activity, String... permissions) {
    ActivityCompat.requestPermissions(activity, permissions, ActivitiesRequestCodes
        .QR_SCANNER_REQUEST_PERMISSION);
  }
}

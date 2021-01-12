package io.iohk.atala.prism.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class PermissionUtils {

    public static boolean checkIfAlreadyHavePermission(Context context, String permission) {
        int result = ContextCompat.checkSelfPermission(context, permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestForSpecificPermission(Activity activity,
                                                    int requestCode, String... permissions) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    public static void requestForSpecificPermission(Fragment fragment,
                                                    int requestCode, String... permissions) {
        fragment.requestPermissions(permissions, requestCode);
    }
}

package io.iohk.cvp.views.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.ActivitiesRequestCodes;
import io.iohk.cvp.utils.IntentDataConstants;
import io.iohk.cvp.utils.PermissionUtils;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.views.Navigator;
import javax.inject.Inject;

public class ConnectionActivity extends CvpActivity<ConnectionsActivityViewModel> {

  @Inject
  Navigator navigator;

  @Inject
  ConnectionsActivityViewModel viewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @OnClick(R.id.scan_qr)
  public void scanQr() {
    if (!PermissionUtils
        .checkIfAlreadyHavePermission(this.getApplicationContext(), Manifest.permission.CAMERA)) {
      PermissionUtils.requestForSpecificPermission(this, ActivitiesRequestCodes
          .QR_SCANNER_REQUEST_PERMISSION, Manifest.permission.CAMERA);
    } else {
      navigator.showQrScanner(this);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY) {
      if (resultCode == RESULT_OK) {
        String token = data.getStringExtra(IntentDataConstants.QR_RESULT);
        viewModel.getIssuerInfo(token).observe(this, issuerInfo -> {
          // TODO update UI
        });
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == ActivitiesRequestCodes
        .QR_SCANNER_REQUEST_PERMISSION) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        navigator.showQrScanner(this);
      } else {
        navigator.showPermissionDeniedPopUp(this);
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  public ConnectionsActivityViewModel getViewModel() {
    return viewModel;
  }

  @Override
  protected Navigator getNavigator() {
    return navigator;
  }

  @Override
  protected int getView() {
    return R.layout.activity_connections;
  }

  @Override
  protected int getTitleValue() {
    return R.string.connections_activity_title;
  }
}
package io.iohk.cvp.views.fragments;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import javax.inject.Inject;

import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.ActivitiesRequestCodes;
import io.iohk.cvp.utils.IntentDataConstants;
import io.iohk.cvp.utils.PermissionUtils;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class FirstConnectionFragment extends CvpFragment<ConnectionsActivityViewModel> {

  @Inject
  Navigator navigator;

  private ViewModelProvider.Factory factory;

  @Inject
  public FirstConnectionFragment(ViewModelProvider.Factory factory) {
    this.factory = factory;
  }

  @Override
  protected int getViewId() {
    return R.layout.fragment_first_connection;
  }

  @Override
  public ConnectionsActivityViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(ConnectionsActivityViewModel.class);
  }

  @OnClick(R.id.scan_qr)
  public void scanQr() {
    if (!PermissionUtils
      .checkIfAlreadyHavePermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA)) {
      PermissionUtils.requestForSpecificPermission(getActivity(), ActivitiesRequestCodes
        .QR_SCANNER_REQUEST_PERMISSION, Manifest.permission.CAMERA);
    } else {
      navigator.showQrScanner(this);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY && resultCode == Activity.RESULT_OK) {
      String token = data.getStringExtra(IntentDataConstants.QR_RESULT);
      ((MainActivity) getActivity()).onNavigation(BottomAppBarOption.CONNECTIONS);
      /*viewModel.getIssuerInfo(token).observe(this, issuerInfo -> {
        // TODO update UI
      });*/
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
        navigator.showPermissionDeniedPopUp(getChildFragmentManager());
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }
}

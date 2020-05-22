package io.iohk.cvp.views.fragments;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.utils.ActivitiesRequestCodes;
import io.iohk.cvp.utils.ActivityUtils;
import io.iohk.cvp.utils.PermissionUtils;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;

import javax.inject.Inject;

import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.ParticipantInfo;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class FirstConnectionFragment extends CvpFragment<ConnectionsActivityViewModel> {


    private ViewModelProvider.Factory factory;
    private int idTitle;

    private List<ConnectionInfo> issuerConnections;

    public static FirstConnectionFragment newInstance(int idTitle, List<ConnectionInfo> issuerConnections) {

        FirstConnectionFragment instance = new FirstConnectionFragment();
        instance.idTitle = idTitle;
        instance.issuerConnections = issuerConnections;

        return instance;
    }

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
        ConnectionsActivityViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(ConnectionsActivityViewModel.class);
        viewModel.setContext(getContext());
        return viewModel;
    }

    private int getTitleId() {
        if (this.idTitle != 0) {
            return this.idTitle;
        } else {
          return R.string.notifications;
        }
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new RootAppBar(getTitleId());
    }

    @OnClick(R.id.scan_qr)
    void scanQr() {
        if (!PermissionUtils
                .checkIfAlreadyHavePermission(getActivity().getApplicationContext(),
                        Manifest.permission.CAMERA)) {
            PermissionUtils.requestForSpecificPermission(getActivity(), ActivitiesRequestCodes
                    .QR_SCANNER_REQUEST_PERMISSION, Manifest.permission.CAMERA);
        } else {
            navigator.showQrScanner(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityUtils
                .onQrcodeResult(requestCode, resultCode, (MainActivity) getActivity(),
                        viewModel, data, this, issuerConnections);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == ActivitiesRequestCodes
                .QR_SCANNER_REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                navigator.showQrScanner(this);
            } else {
                navigator.showPopUp(getChildFragmentManager(), null);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}

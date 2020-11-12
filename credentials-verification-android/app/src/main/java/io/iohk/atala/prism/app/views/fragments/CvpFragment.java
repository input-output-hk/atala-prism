package io.iohk.atala.prism.app.views.fragments;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.common.SupportErrorDialogFragment;

import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import io.iohk.atala.prism.app.utils.ActivitiesRequestCodes;
import io.iohk.atala.prism.app.utils.PermissionUtils;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.views.Navigator;
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator;

import java.util.Optional;

import javax.inject.Inject;

import io.iohk.atala.prism.app.views.fragments.utils.CommonUtils;
import lombok.Getter;

public abstract class CvpFragment<T extends ViewModel> extends DaggerFragment {

    T viewModel;

    @Getter
    @Inject
    Navigator navigator;

    private ProgressDialog mProgressDialog;

    public abstract T getViewModel();

    protected abstract AppBarConfigurator getAppBarConfigurator();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        android.view.View view = inflater.inflate(getViewId(), container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewModel = getViewModel();
    }

    public void showLoading() {
        hideLoading();
        mProgressDialog = CommonUtils.showLoadingDialog(this.getContext());
    }

    public void hideLoading() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setActionBar();
    }

    protected void setActionBar() {
        Optional<ActionBar> supportActionBar = activity().map(AppCompatActivity::getSupportActionBar);
        supportActionBar.ifPresent(actionBar -> {
            if (getAppBarConfigurator() != null) {
                getAppBarConfigurator().configureActionBar(actionBar);
            }
        });
    }

    protected void setActionBarTitle(int title) {
        Optional<ActionBar> supportActionBar = activity().map(AppCompatActivity::getSupportActionBar);
        supportActionBar.ifPresent(actionBar -> {
            actionBar.setTitle(title);
        });
    }

    public Optional<AppCompatActivity> activity() {
        return Optional.ofNullable(getActivity())
                .map(fragmentActivity -> (AppCompatActivity) fragmentActivity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    protected abstract int getViewId();

    protected void showGenericError() {
        FragmentManager fm = getFragmentManager();
        SupportErrorDialogFragment.newInstance(new Dialog(getContext()))
                .show(fm, "");
        getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                R.string.server_error_message));
    }

    protected void scanQr() {
        if (!PermissionUtils
                .checkIfAlreadyHavePermission(getActivity().getApplicationContext(),
                        Manifest.permission.CAMERA)) {
            PermissionUtils.requestForSpecificPermission(this, ActivitiesRequestCodes
                    .QR_SCANNER_REQUEST_PERMISSION, Manifest.permission.CAMERA);
        } else {
            navigator.showQrScanner(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ActivitiesRequestCodes.QR_SCANNER_REQUEST_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                navigator.showQrScanner(this);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

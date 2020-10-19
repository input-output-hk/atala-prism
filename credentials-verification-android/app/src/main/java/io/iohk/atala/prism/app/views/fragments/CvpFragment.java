package io.iohk.atala.prism.app.views.fragments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.common.SupportErrorDialogFragment;

import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
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
}

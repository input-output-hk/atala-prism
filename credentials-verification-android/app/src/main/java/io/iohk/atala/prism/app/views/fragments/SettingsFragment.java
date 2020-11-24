package io.iohk.atala.prism.app.views.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.common.SupportErrorDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.atala.prism.app.grpc.AsyncTaskResult;
import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.utils.FirebaseAnalyticsEvents;
import io.iohk.atala.prism.app.viewmodel.ConnectionsActivityViewModel;
import io.iohk.atala.prism.app.views.Navigator;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;
import io.iohk.atala.prism.app.views.activities.MainActivity;
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator;
import io.iohk.atala.prism.app.views.fragments.utils.RootAppBar;
import io.iohk.atala.prism.app.views.utils.components.OptionItem;
import io.iohk.atala.prism.app.views.utils.dialogs.SuccessDialog;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class SettingsFragment extends CvpFragment<ConnectionsActivityViewModel> implements DeleteAllConnectionsDialogFragment.OnResetDataListener {

    // Request code is obligatory on setTargetFragment() even if is not used for transfer data back
    private static final int DELETE_ALL_CONNECTIONS_REQUEST_CODE = 22;

    public static final String SECURITY_SELECTED_TRANSACTION = "securitySelectedTransaction";

    @Inject
    Navigator navigator;

    @BindView(R.id.backend_ip)
    OptionItem backendConfigItem;

    @Inject
    ViewModelProvider.Factory factory;

    @Override
    protected int getViewId() {
        return R.layout.fragment_settings;
    }

    @Override
    public ConnectionsActivityViewModel getViewModel() {
        ConnectionsActivityViewModel viewModel = ViewModelProviders.of(this, factory).get(ConnectionsActivityViewModel.class);
        viewModel.setContext(getContext());
        return viewModel;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new RootAppBar(R.string.settings);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (!BuildConfig.DEBUG) {
            backendConfigItem.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObservers();
    }

    private void initObservers() {
        MutableLiveData<AsyncTaskResult<Boolean>> liveData = getViewModel().getRemoveAllDataLiveData();

        liveData.observe(getViewLifecycleOwner(), response -> {
            try {
                if (response.getError() != null) {
                    FragmentManager fm = requireActivity().getSupportFragmentManager();
                    SupportErrorDialogFragment.newInstance(new Dialog(getContext()))
                            .show(fm, "");
                    getNavigator().showPopUp(requireActivity().getSupportFragmentManager(), getResources().getString(
                            R.string.server_error_message));
                    return;
                }

                if (response.getResult()) {
                    SuccessDialog.newInstance(this, R.string.connections_remove_success, true)
                            .show(requireActivity().getSupportFragmentManager(), "dialog");
                    getViewModel().getRemoveAllDataLiveData().setValue(new AsyncTaskResult(false));
                }
            } catch (Exception ex) {
                FirebaseCrashlytics.getInstance().recordException(ex);
            } finally {
                hideLoading();
            }
        });
    }

    @OnClick(R.id.support)
    void onSupportClick() {
        navigator.showWebView(requireActivity());
    }

    @OnClick(R.id.backend_ip)
    void onBackendIpClick() {
        navigator.showFragmentOnTopOfMenu(requireActivity().getSupportFragmentManager(), new BackendIpFragment());
    }

    @OnClick(R.id.security)
    void onSecurityClick() {
        if (!new Preferences(getContext()).isPinConfigured()) {
            navigator.showFragmentOnTopOfMenu(requireActivity().getSupportFragmentManager(), new SecuritySettingsStep1Fragment());
        } else {
            navigator.showFragmentOnTopOfMenu(requireActivity().getSupportFragmentManager(), new SecurityFragment());
        }
    }

    @OnClick(R.id.about)
    void onAboutClick() {
        ((MainActivity) getActivity()).sentFirebaseAnalyticsEvent(FirebaseAnalyticsEvents.SUPPORT);
        navigator.showFragmentOnTopOfMenu(requireActivity().getSupportFragmentManager(), new AboutFragment());
    }


    @OnClick(R.id.delete_credentials)
    void onDeleteCredentialsClicked() {
        DeleteAllConnectionsDialogFragment deleteAllConnectionsDialogFragment = DeleteAllConnectionsDialogFragment.newInstance();
        deleteAllConnectionsDialogFragment.setTargetFragment(this, DELETE_ALL_CONNECTIONS_REQUEST_CODE);
        getNavigator().showDialogFragment(requireActivity().getSupportFragmentManager(),
                deleteAllConnectionsDialogFragment, "DELETE_CONNECTIONS_DIALOG_FRAGMENT");
    }

    @OnClick(R.id.custom_date)
    void onCustomDateClicked() {
        SettingsDateFormatFragment fragment = new SettingsDateFormatFragment();
        getNavigator().showFragmentOnTop(requireActivity().getSupportFragmentManager(), fragment);
    }

    @Override
    public void resetData() {
        ((MainActivity) getActivity()).sentFirebaseAnalyticsEvent(FirebaseAnalyticsEvents.RESET_DATA);
        showLoading();
        getViewModel().removeAllLocalData();
    }
}
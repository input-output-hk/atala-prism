package io.iohk.atala.prism.app.ui.main.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import javax.inject.Inject;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.support.DaggerFragment;
import io.iohk.atala.prism.app.grpc.AsyncTaskResult;
import io.iohk.atala.prism.app.neo.common.extensions.FragmentActivityExtensionsKt;
import io.iohk.atala.prism.app.neo.common.extensions.FragmentExtensionsKt;
import io.iohk.atala.prism.app.ui.commondialogs.SuccessDialog;
import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.utils.FirebaseAnalyticsEvents;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;
import io.iohk.atala.prism.app.ui.utils.components.OptionItem;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class SettingsFragment extends DaggerFragment {

    public static final String SECURITY_SELECTED_TRANSACTION = "securitySelectedTransaction";

    @BindView(R.id.backend_ip)
    OptionItem backendConfigItem;

    @Inject
    ViewModelProvider.Factory factory;

    @BindView(R.id.pay_id)
    OptionItem payIdItem;

    public SettingsViewModel getViewModel() {
        SettingsViewModel viewModel = ViewModelProviders.of(this, factory).get(SettingsViewModel.class);
        return viewModel;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Handle DeleteAllConnectionsDialogFragment response
        getParentFragmentManager().setFragmentResultListener(ResetDataDialogFragment.REQUEST_DELETE_DATA, this, (requestKey, bundle) -> {
            int result = bundle.getInt(FragmentExtensionsKt.getKEY_RESULT(this));
            if(result == Activity.RESULT_OK){
                resetData();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, view);
        if (!BuildConfig.DEBUG) {
            backendConfigItem.setVisibility(View.GONE);
        }
        // TODO We need to migrate to a "Fragment-owned App Bar" see: https://developer.android.com/guide/fragments/appbar#fragment
        FragmentExtensionsKt.getSupportActionBar(this).show();
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
                    FragmentActivityExtensionsKt.showErrorDialog(requireActivity(), R.string.server_error_message);
                    return;
                }

                if (response.getResult()) {
                    new SuccessDialog.Builder(requireContext()).setSecondaryText(R.string.connections_remove_success).build()
                            .show(requireActivity().getSupportFragmentManager(), "dialog");
                    getViewModel().getRemoveAllDataLiveData().setValue(new AsyncTaskResult(false));
                }
            } catch (Exception ex) {
                FirebaseCrashlytics.getInstance().recordException(ex);
            } finally {
                FragmentActivityExtensionsKt.hideBlockUILoading(requireActivity());
            }
        });

        getViewModel().getPayIdOptionEnable().observe(getViewLifecycleOwner(), enabled -> {
            payIdItem.setEnabled(enabled);
        });
    }

    @OnClick(R.id.support)
    void onSupportClick() {
        Navigation.findNavController(requireView()).navigate(R.id.action_settingsFragment_to_webViewActivity);
    }

    @OnClick(R.id.backend_ip)
    void onBackendIpClick() {
        Navigation.findNavController(requireView()).navigate(R.id.action_settingsFragment_to_backendIpFragment);
    }

    @OnClick(R.id.security)
    void onSecurityClick() {
        if (!new Preferences(getContext()).isPinConfigured()) {
            Navigation.findNavController(requireView()).navigate(R.id.action_settingsFragment_to_securitySettingsStep1Fragment);
        } else {
            Navigation.findNavController(requireView()).navigate(R.id.action_settingsFragment_to_securityFragment);
        }
    }

    @OnClick(R.id.about)
    void onAboutClick() {
        FirebaseAnalytics.getInstance(requireContext()).logEvent(FirebaseAnalyticsEvents.SUPPORT, null);
        Navigation.findNavController(requireView()).navigate(R.id.action_settingsFragment_to_aboutFragment);
    }


    @OnClick(R.id.delete_credentials)
    void onDeleteCredentialsClicked() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_settingsFragment_to_resetDataDialogFragment);
    }

    @OnClick(R.id.custom_date)
    void onCustomDateClicked() {
        Navigation.findNavController(requireView()).navigate(R.id.action_settingsFragment_to_settingsDateFormatFragment);
    }

    @OnClick(R.id.pay_id)
    void onPayIdClicked() {
        Navigation.findNavController(requireView()).navigate(R.id.action_settingsFragment_to_payIdObtainingNavActivity);
    }
    @OnClick(R.id.verified_identity)
    void onVerifiedIdentityClicked() {
        Navigation.findNavController(requireView()).navigate(R.id.action_settingsFragment_to_idVerificationNavActivity2);
    }

    private void resetData() {
        FirebaseAnalytics.getInstance(requireContext()).logEvent(FirebaseAnalyticsEvents.RESET_DATA, null);
        FragmentActivityExtensionsKt.showBlockUILoading(requireActivity());
        getViewModel().removeAllLocalData();
    }
}
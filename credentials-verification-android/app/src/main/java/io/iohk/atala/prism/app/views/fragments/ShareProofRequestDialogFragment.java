package io.iohk.atala.prism.app.views.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.common.SupportErrorDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.stream.Collectors;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import dagger.android.support.AndroidSupportInjection;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.data.local.db.model.Credential;
import io.iohk.atala.prism.app.utils.ImageUtils;
import io.iohk.atala.prism.app.viewmodel.ConnectionsListablesViewModel;
import io.iohk.atala.prism.app.viewmodel.dtos.CredentialsToShare;
import io.iohk.atala.prism.app.views.activities.MainActivity;
import io.iohk.atala.prism.app.views.utils.components.ShareCredentialRow;
import io.iohk.atala.prism.app.views.utils.components.bottomAppBar.BottomAppBarOption;
import io.iohk.atala.prism.app.views.utils.dialogs.SuccessDialog;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ShareProofRequestDialogFragment extends CvpDialogFragment<ConnectionsListablesViewModel> {

    public static final String SHARE_PROOF_REQUEST_DIALOG = "SHARE_PROOF_REQUEST_DIALOG_FRAGMENT";

    @BindView(R.id.title)
    TextView titleTextView;

    @BindView(R.id.participantLogo)
    ImageView participantLogoImgView;

    @BindView(R.id.credentailsToShareContent)
    LinearLayout credentailsToShareContent;

    @BindView(R.id.share_button)
    Button shareButton;

    @Inject
    ViewModelProvider.Factory factory;

    private CredentialsToShare credentialsToShare;

    @Inject
    ShareProofRequestDialogFragment(ViewModelProvider.Factory factory) {
        this.factory = factory;
    }

    public static CvpDialogFragment newInstance(CredentialsToShare credentialsToShare) {

        ShareProofRequestDialogFragment instance = new ShareProofRequestDialogFragment();
        instance.setCancelable(false);
        instance.credentialsToShare = credentialsToShare;

        return instance;
    }

    @Override
    protected int getViewId() {
        return R.layout.share_proof_request_dialog_fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        try {
            participantLogoImgView.setImageBitmap(
                    ImageUtils.getBitmapFromByteArray(credentialsToShare.getConnection().logo));
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        try {
            titleTextView.setText(credentialsToShare.getConnection().name);
            for (Credential acceptedCredential : credentialsToShare.getCredentialsToShare()) {
                TextView text = new TextView(getActivity());
                text.setText(CredentialUtil.getType(acceptedCredential, getContext()));
                credentailsToShareContent.addView(new ShareCredentialRow(this, acceptedCredential));
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObservers();
    }

    private void initObservers() {
        getViewModel().getMessageSentLiveData().observe(getViewLifecycleOwner(), asyncTaskResult -> {
            if (asyncTaskResult.getError() != null) {
                FragmentManager fm = getFragmentManager();
                SupportErrorDialogFragment.newInstance(new Dialog(getContext())).show(fm, "");
                getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                        R.string.server_error_message));
                return;
            }
            if (asyncTaskResult.getResult()) {
                SuccessDialog.newInstance(this, R.string.server_share_successfully)
                        .show(getFragmentManager(), "dialog");
                this.dismiss();
                ((MainActivity) getActivity()).onNavigation(BottomAppBarOption.CONTACTS);
            }
        });
        getViewModel().getContactUpdatedLiveData().observe(getViewLifecycleOwner(), asyncTaskResult -> {
            if (asyncTaskResult.getResult()) {
                getActivity().onBackPressed();
                dismiss();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        if (window == null) {
            return;
        }

        WindowManager.LayoutParams params = window.getAttributes();
        float factor = getContext().getResources().getDisplayMetrics().density;
        params.width = (int) (350 * factor);
        params.height = (int) (330 * factor);
        window.setAttributes(params);
    }

    @Override
    public ConnectionsListablesViewModel getViewModel() {
        ConnectionsListablesViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(ConnectionsListablesViewModel.class);
        viewModel.setContext(getContext());
        return viewModel;
    }

    @OnClick(R.id.cancel_button)
    public void onCancelClick() {
        getViewModel().setLastMessageSeen(credentialsToShare.getConnection().connectionId, credentialsToShare.getMessageId());
    }

    public void enableShareButton() {
        for (int i = 0; i < credentailsToShareContent.getChildCount(); i++) {
            ShareCredentialRow shareCredentialRow = (ShareCredentialRow) credentailsToShareContent.getChildAt(i);
            if (!shareCredentialRow.isChecked()) {
                shareButton.setEnabled(false);
                return;
            }
        }
        shareButton.setEnabled(true);
    }

    @OnClick(R.id.share_button)
    public void onConnectClick() {
        getViewModel().acceptProofRequest(credentialsToShare.getConnection().keyDerivationPath,
                credentialsToShare.getConnection(), credentialsToShare.getCredentialsToShare());
    }
}

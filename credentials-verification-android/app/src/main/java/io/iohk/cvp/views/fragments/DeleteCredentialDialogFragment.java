package io.iohk.cvp.views.fragments;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.common.SupportErrorDialogFragment;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.core.enums.CredentialType;
import io.iohk.cvp.utils.CredentialParse;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.viewmodel.dtos.CredentialDto;
import io.iohk.cvp.views.utils.dialogs.SuccessDialog;
import lombok.NoArgsConstructor;

import static io.iohk.cvp.utils.IntentDataConstants.CREDENTIAL_DATA_KEY;
import static io.iohk.cvp.utils.IntentDataConstants.CREDENTIAL_ID_KEY;
import static io.iohk.cvp.utils.IntentDataConstants.CREDENTIAL_TYPE_KEY;

// TODO This needs its own [ViewModel]
@NoArgsConstructor
public class DeleteCredentialDialogFragment extends CvpDialogFragment<CredentialsViewModel> {
    private static final float DELETE_ALL_CONNECTIONS_DIALOG_WIDTH = 350;
    private static final float DELETE_ALL_CONNECTIONS_DIALOG_HEIGHT = 300;
    private DeleteCredentialDialogFragment.OnDeleteCredential onInputListener;
    private String credentialId;

    @BindView(R.id.credential_text)
    TextView credentialText;

    @BindView(R.id.credential_logo)
    ImageView credentialLogo;

    @Inject
    ViewModelProvider.Factory factory;

    @OnClick(R.id.cancel_button)
    void cancel() {
        this.dismiss();
    }

    @OnClick(R.id.delete_button)
    void deleteCredential() {
        getViewModel().deleteCredential(credentialId);
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        float factor = getContext().getResources().getDisplayMetrics().density;
        params.width = (int) (DELETE_ALL_CONNECTIONS_DIALOG_WIDTH * factor);
        params.height = (int) (DELETE_ALL_CONNECTIONS_DIALOG_HEIGHT * factor);
        window.setAttributes(params);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        View view = super.onCreateView(inflater, container, savedInstanceState);
        credentialId = getArguments().getString(CREDENTIAL_ID_KEY);

        CredentialDto credential = CredentialParse.parse(getArguments().getString(CREDENTIAL_TYPE_KEY), getArguments().getString(CREDENTIAL_DATA_KEY));
        credentialText.setText(credential.getTitle());
        credentialText.setTextColor(getResources().getColor(R.color.black, null));
        credentialLogo.setImageDrawable(CredentialUtil.getLogo(credential.getCredentialType(), getContext()));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObservers();
    }

    private void initObservers() {
        getViewModel().getDeleteCredentialLiveData().observe(getViewLifecycleOwner(), credentialDeleted -> {
            if (credentialDeleted) {
                onInputListener.credentialDeleted();
                this.dismiss();
            }
        });

        getViewModel().getShowErrorMessageLiveData().observe(getViewLifecycleOwner(), showErrorMessage -> {
            FragmentManager fm = getFragmentManager();
            SupportErrorDialogFragment.newInstance(new Dialog(getContext()))
                    .show(fm, "");
            getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                    R.string.server_error_message));
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        onInputListener = (DeleteCredentialDialogFragment.OnDeleteCredential) getTargetFragment();
    }

    public interface OnDeleteCredential {
        void credentialDeleted();
    }

    @Override
    protected int getViewId() {
        return R.layout.component_delete_credential_dialog;
    }

    @Override
    public CredentialsViewModel getViewModel() {
        return ViewModelProviders.of(this, factory).get(CredentialsViewModel.class);
    }

}
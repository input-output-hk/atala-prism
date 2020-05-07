package io.iohk.cvp.views.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.SupportErrorDialogFragment;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.core.enums.CredentialType;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.utils.ImageUtils;
import io.iohk.cvp.viewmodel.ConnectionsListablesViewModel;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.utils.components.ShareCredentialRow;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import io.iohk.cvp.views.utils.dialogs.SuccessDialog;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.Credential;
import io.iohk.prism.protos.ProofRequest;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ShareProofRequestDialogFragment extends CvpDialogFragment<ConnectionsListablesViewModel> {

    private static final String TYPES_KEY = "types";
    private static final String TOKEN_KEY = "token";
    private static final String NAME_KEY = "participantName";
    private static final String LOGO_DATA_KEY = "logo";

    @BindView(R.id.title)
    TextView titleTextView;

    @BindView(R.id.participantLogo)
    ImageView participantLogoImgView;

    @BindView(R.id.credentialTypesContent)
    LinearLayout credentialTypesContent;

    @BindView(R.id.credentailsToShareContent)
    LinearLayout credentailsToShareContent;

    private ViewModelProvider.Factory factory;

    private ProofRequest proofRequest;

    private List<Credential> acceptedcredentials;

    private ConnectionInfo connection;

    private LiveData<AsyncTaskResult<Boolean>> liveData;

    private int sharedCedentialsCount;

    public static ShareProofRequestDialogFragment newInstance(ProofRequest proofRequest,
                                                              List<Credential> acceptedcredentials,
                                                              ConnectionInfo connection) {

        ShareProofRequestDialogFragment instance = new ShareProofRequestDialogFragment();
        instance.setCancelable(false);
        instance.proofRequest = proofRequest;
        instance.acceptedcredentials = acceptedcredentials;
        instance.connection = connection;

        return instance;
    }

    @Inject
    ShareProofRequestDialogFragment(ViewModelProvider.Factory factory) {
        this.factory = factory;
    }

    @Override
    protected int getViewId() {
        return R.layout.share_proof_request_dialog_fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        participantLogoImgView.setImageBitmap(
                                ImageUtils.getBitmapFromByteArray(connection.getParticipantInfo().getIssuer().getLogo().toByteArray()));

        try {
            titleTextView.setText(connection.getParticipantInfo().getIssuer().getName());
            for (Credential acceptedCredential : acceptedcredentials) {
                TextView text = new TextView(getActivity());
                text.setText(getCredentialType(acceptedCredential));
                credentialTypesContent.addView(text);
                credentailsToShareContent.addView(new ShareCredentialRow(getActivity(), acceptedCredential));
            }

        } catch (Exception e) {
            Crashlytics.logException(e);
        }

        return view;
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
        params.height = (int) (400 * factor);
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
        Preferences prefs = new Preferences(getContext());
        prefs.saveMessage(proofRequest.getConnectionToken(), Preferences.PROOF_REQUEST_CANCEL_KEY);
        getActivity().onBackPressed();
        this.dismiss();
    }

    @OnClick(R.id.share_button)
    public void onConnectClick() {

        boolean isAllCredentialsChecked = true;
        for(int i=0; i<credentailsToShareContent.getChildCount(); i++) {
            ShareCredentialRow shareCredentialRow = (ShareCredentialRow) credentailsToShareContent.getChildAt(i);
            if(!shareCredentialRow.isChecked()){
                isAllCredentialsChecked = false;
                break;
            }
        }

        if(isAllCredentialsChecked) {
            Preferences prefs = new Preferences(getContext());
            sharedCedentialsCount = 0;
            acceptedcredentials.forEach(credential -> {
                try {
                    sharedCedentialsCount++;
                    String userId = prefs.getUserIdByConnection(connection.getConnectionId()).orElseThrow(() ->
                            new SharedPrefencesDataNotFoundException(
                                    "Couldn't find user id for connection id " + connection.getConnectionId(),
                                    ErrorCode.USER_ID_NOT_FOUND));

                    liveData = viewModel.sendMessage(userId, connection.getConnectionId(), credential.toByteString());

                    if (!liveData.hasActiveObservers()) {
                        liveData.observe(this, response -> {
                            FragmentManager fm = getFragmentManager();
                            if (response.getError() != null) {
                                SupportErrorDialogFragment.newInstance(new Dialog(getContext())).show(fm, "");
                                getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                                        R.string.server_error_message));
                                return;
                            } else {
                                if (acceptedcredentials.size() == sharedCedentialsCount) {

                                    prefs.saveMessage(proofRequest.getConnectionToken(), Preferences.PROOF_REQUEST_SHARED_KEY);
                                    SuccessDialog.newInstance(this, R.string.server_share_successfully)
                                            .show(getFragmentManager(), "dialog");
                                    this.dismiss();
                                    ((MainActivity) getActivity()).onNavigation(BottomAppBarOption.HOME, null);
                                }

                            }
                        });
                    }

                } catch (Exception e) {
                    Crashlytics.logException(e);
                }
            });
        }else{
            Toast.makeText(getActivity(), R.string.select_all_credential_alert, Toast.LENGTH_SHORT).show();
        }
    }

    private String getCredentialType(Credential credential){
        Optional<CredentialType> credentialTypeOptional = CredentialType.getByValue(credential.getTypeId());
        if(credentialTypeOptional.isPresent()) {
            switch (credentialTypeOptional.get().getId()) {
                case 1:
                    return getResources().getString(R.string.credential_government_type_title);
                case 2:
                    return getResources().getString(R.string.credential_degree_type_title);
                case 3:
                    return getResources().getString(R.string.credential_employed_type_title);
                case 4:
                    return getResources().getString(R.string.credential_insurance_type_title);
                default:
                    return "";
            }
        }
        return "";
    }

    private Drawable getCredentialLogo(Credential credential){
        Optional<CredentialType> credentialTypeOptional = CredentialType.getByValue(credential.getTypeId());
        if(credentialTypeOptional.isPresent()) {
            switch (credentialTypeOptional.get().getId()) {
                case 1:
                    return getResources().getDrawable(R.drawable.ic_id_government);
                case 2:
                    return getResources().getDrawable(R.drawable.ic_id_university);
                case 3:
                    return getResources().getDrawable(R.drawable.ic_id_proof);
                case 4:
                    return getResources().getDrawable(R.drawable.ic_id_insurance);
                default:
                    return null;
            }
        }
        return null;
    }
}

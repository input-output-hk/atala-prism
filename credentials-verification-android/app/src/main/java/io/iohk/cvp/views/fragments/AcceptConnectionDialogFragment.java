package io.iohk.cvp.views.fragments;

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
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.FirebaseAnalyticsEvents;
import io.iohk.cvp.utils.ImageUtils;
import io.iohk.cvp.viewmodel.AcceptConnectionViewModel;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.ParticipantInfo;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AcceptConnectionDialogFragment extends CvpDialogFragment<AcceptConnectionViewModel> {

    private static final String TITLE_KEY = "title";
    private static final String BUTTON_KEY = "button";
    private static final String TOKEN_KEY = "token";
    private static final String NAME_KEY = "participantName";
    private static final String LOGO_DATA_KEY = "logo";

    @BindView(R.id.title)
    TextView titleTextView;

    @BindView(R.id.participantName)
    TextView participantNameTextView;

    @BindView(R.id.connect_button)
    Button connectButton;

    @BindView(R.id.participantLogo)
    ImageView participantLogo;

    private ViewModelProvider.Factory factory;

    public static AcceptConnectionDialogFragment newInstance(String title, String buttonDescription,
                                                             String token, ParticipantInfo participantInfo) {

        AcceptConnectionDialogFragment instance = new AcceptConnectionDialogFragment();

        boolean isIssuer = participantInfo.getParticipantCase().getNumber() == ParticipantInfo.ParticipantCase.ISSUER.getNumber();

        Bundle args = new Bundle();
        args.putString(TITLE_KEY, title);
        args.putString(BUTTON_KEY, buttonDescription);
        args.putString(TOKEN_KEY, token);
        args.putString(NAME_KEY, isIssuer ? participantInfo.getIssuer().getName()
                : participantInfo.getVerifier().getName());
        args.putByteArray(LOGO_DATA_KEY, isIssuer ? participantInfo.getIssuer().getLogo().toByteArray()
                : participantInfo.getVerifier().getLogo().toByteArray());

        instance.setArguments(args);
        instance.setCancelable(false);

        return instance;
    }

    @Inject
    AcceptConnectionDialogFragment(ViewModelProvider.Factory factory) {
        this.factory = factory;
    }

    @Override
    protected int getViewId() {
        return R.layout.accept_connection_dialog_fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        titleTextView.setText(getArguments().getString(TITLE_KEY));
        participantNameTextView.setText(getArguments().getString(NAME_KEY));
        connectButton.setText(getArguments().getString(BUTTON_KEY));

        try{
            participantLogo.setImageBitmap(ImageUtils.getBitmapFromByteArray(getArguments().getByteArray(LOGO_DATA_KEY)));
        }catch (Exception e){
            //NOTHIG
            //ByteArray is null or the image could not be decoded.
        }
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        float factor = getContext().getResources().getDisplayMetrics().density;
        params.width = (int) (350 * factor);
        params.height = (int) (200 * factor);
        window.setAttributes(params);
    }

    @Override
    public AcceptConnectionViewModel getViewModel() {
        AcceptConnectionViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(AcceptConnectionViewModel.class);
        viewModel.setContext(getContext());
        return viewModel;
    }

    @OnClick(R.id.cancel_button)
    public void onCancelClick() {
        ((MainActivity)getActivity()).sentFirebaseAnalyticsEvent(FirebaseAnalyticsEvents.NEW_CREDENTIAL_DECLINE);
        this.dismiss();
    }

    @OnClick(R.id.connect_button)
    public void onConnectClick() {
        ((MainActivity)getActivity()).sentFirebaseAnalyticsEvent(FirebaseAnalyticsEvents.NEW_CREDENTIAL_CONFIRM);
        viewModel.getTokenizationKey().observe(this, response -> {
            this.dismiss();

            if (response.getError() != null) {
                getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                        R.string.server_error_message));
                return;
            }

            Preferences pref = new Preferences(getContext());
            pref.saveConnectionTokenToAccept(getArguments().getString(TOKEN_KEY));
            ((MainActivity) getActivity()).acceptConnection(getArguments().getString(TOKEN_KEY), pref);
        });
    }
}

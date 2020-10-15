package io.iohk.cvp.views.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.ImageUtils;
import io.iohk.atala.prism.protos.ParticipantInfo;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AlreadyConnectedDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlreadyConnectedDialogFragment extends CvpDialogFragment {
    private static final String ISSUER_NAME = "issuerName";
    private static final String LOGO_DATA_KEY = "logoDataKey";

    private static final float ALREADY_CONNECTED_DIALOG_WIDTH = 350;
    private static final float ALREADY_CONNECTED_DIALOG_HEIGHT = 250;

    @BindView(R.id.participantName)
    TextView participantNameTextView;

    @BindView(R.id.ok_button)
    Button connectButton;

    @BindView(R.id.participantLogo)
    ImageView participantLogo;

    private String issuerName;

    public AlreadyConnectedDialogFragment() {
        // Required empty public constructor
    }

    public static AlreadyConnectedDialogFragment newInstance(ParticipantInfo participantInfo) {
        AlreadyConnectedDialogFragment fragment = new AlreadyConnectedDialogFragment();
        Bundle args = new Bundle();
        boolean isIssuer = participantInfo.getParticipantCase().getNumber() == ParticipantInfo.ParticipantCase.ISSUER.getNumber();

        args.putString(ISSUER_NAME, isIssuer ? participantInfo.getIssuer().getName()
                : participantInfo.getVerifier().getName());
        args.putByteArray(LOGO_DATA_KEY, isIssuer ? participantInfo.getIssuer().getLogo().toByteArray()
                : participantInfo.getVerifier().getLogo().toByteArray());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            issuerName = getArguments().getString(ISSUER_NAME);
            participantNameTextView.setText(issuerName);
            try {
                participantLogo.setImageBitmap(ImageUtils.getBitmapFromByteArray(getArguments().getByteArray(LOGO_DATA_KEY)));
            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        float factor = getContext().getResources().getDisplayMetrics().density;
        params.width = (int) (ALREADY_CONNECTED_DIALOG_WIDTH * factor);
        params.height = (int) (ALREADY_CONNECTED_DIALOG_HEIGHT * factor);
        window.setAttributes(params);
    }

    @OnClick(R.id.ok_button)
    public void onConnectClick() {
        this.dismiss();
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_already_connected_dialog;
    }

    @Override
    public ViewModel getViewModel() {
        return null;
    }
}

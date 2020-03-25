package io.iohk.cvp.views.fragments;

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

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.ImageUtils;
import io.iohk.cvp.viewmodel.AcceptConnectionViewModel;
import io.iohk.cvp.views.Preferences;
import io.iohk.prism.protos.ParticipantInfo;
import io.iohk.prism.protos.ParticipantInfo.ParticipantCase;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AcceptConnectionDialogFragment extends CvpDialogFragment<AcceptConnectionViewModel> {

  private static final String TITLE_KEY = "title";
  private static final String TOKEN_KEY = "token";
  private static final String NAME_KEY = "participantName";
  private static final String LOGO_DATA_KEY = "logo";

  @BindView(R.id.title)
  TextView titleTextView;

  @BindView(R.id.participantName)
  TextView participantNameTextView;

  @BindView(R.id.participantLogo)
  ImageView participantLogoImgView;

  private ViewModelProvider.Factory factory;

  public static AcceptConnectionDialogFragment newInstance(String token,
      ParticipantInfo participantInfo) {
    AcceptConnectionDialogFragment instance = new AcceptConnectionDialogFragment();

    boolean isIssuer =
        participantInfo.getParticipantCase().getNumber() == ParticipantCase.ISSUER.getNumber();

    Bundle args = new Bundle();
    args.putString(TOKEN_KEY, token);
    args.putString(NAME_KEY, isIssuer ? participantInfo.getIssuer().getName()
        : participantInfo.getVerifier().getName());
    args.putString(TITLE_KEY, isIssuer ? "University name" : "Employer name");
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
    participantLogoImgView.setImageBitmap(
        ImageUtils.getBitmapFromByteArray(getArguments().getByteArray(LOGO_DATA_KEY)));
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
    params.height = (int) (250 * factor);
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
    this.dismiss();
  }

  @OnClick(R.id.connect_button)
  public void onConnectClick() {
    viewModel.getTokenizationKey().observe(this, response -> {
      this.dismiss();

      if (response.getError() != null) {
        getNavigator().showPopUp(getFragmentManager(), getResources().getString(
            R.string.server_error_message));
        return;
      }

      Preferences prefs = new Preferences(getContext());
      prefs.saveConnectionTokenToAccept(getArguments().getString(TOKEN_KEY));
      navigator.showPayment(getActivity(), response.getResult());
    });
  }
}

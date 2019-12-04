package io.iohk.cvp.views.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.OnClick;
import com.crashlytics.android.Crashlytics;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.CryptoException;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.io.connector.ParticipantInfo;
import io.iohk.cvp.utils.CryptoUtils;
import io.iohk.cvp.viewmodel.AcceptConnectionViewModel;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import java.security.spec.InvalidKeySpecException;
import javax.inject.Inject;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AcceptConnectionDialogFragment extends CvpDialogFragment<AcceptConnectionViewModel> {

  private static final String TITLE_KEY = "title";
  private static final String TOKEN_KEY = "token";
  private static final String NAME_KEY = "participantName";

  @BindView(R.id.title)
  TextView titleTextView;

  @BindView(R.id.participantName)
  TextView participantNameTextView;

  private ViewModelProvider.Factory factory;

  public static AcceptConnectionDialogFragment newInstance(String token,
      ParticipantInfo participantInfo) {
    AcceptConnectionDialogFragment instance = new AcceptConnectionDialogFragment();

    boolean isIssuer = participantInfo.getIssuer() != null;

    Bundle args = new Bundle();
    args.putString(TOKEN_KEY, token);
    args.putString(NAME_KEY, isIssuer ? participantInfo.getIssuer().getName()
        : ""); // TODO replace "" with verifier's name
    args.putString(TITLE_KEY, isIssuer ? "University name" : "Employer name");
    // TODO handle logo based on inititor type
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
    return ViewModelProviders.of(this, factory).get(AcceptConnectionViewModel.class);
  }

  @OnClick(R.id.cancel_button)
  public void onCancelClick() {
    this.dismiss();
  }

  @OnClick(R.id.connect_button)
  public void onConnectClick() {
    Preferences prefs = new Preferences(getContext());
    try {
      viewModel
          .addConnectionFromToken(getArguments().getString(TOKEN_KEY),
              CryptoUtils.getPublicKey(prefs))
          .observe(this, connectionInfo -> {
            prefs.saveUserId(connectionInfo.getUserId());
            this.dismiss();
            ((MainActivity) getActivity())
                .onNavigation(BottomAppBarOption.CONNECTIONS, connectionInfo.getUserId());
          });
    } catch (SharedPrefencesDataNotFoundException | InvalidKeySpecException | CryptoException e) {
      Crashlytics.logException(e);
      // TODO show error message
    }
  }
}

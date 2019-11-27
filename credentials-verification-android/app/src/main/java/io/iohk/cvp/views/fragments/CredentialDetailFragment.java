package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.OnClick;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.button.MaterialButton;
import com.google.protobuf.InvalidProtocolBufferException;
import io.iohk.cvp.R;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.StackedAppBar;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class CredentialDetailFragment extends CvpFragment<CredentialsViewModel> {

  private ViewModelProvider.Factory factory;

  @Setter
  private String credentialId;

  @Setter
  private Boolean credentialIsNew;

  @BindView(R.id.decline_credential)
  public MaterialButton declineButton;

  @BindView(R.id.accept_credential)
  public Button acceptButton;

  @BindView(R.id.text_view_university_name)
  TextView textViewUniversityName;

  @BindView(R.id.text_view_full_name)
  TextView textViewFullName;

  @BindView(R.id.text_view_credential_name)
  TextView textViewCredentialName;

  @Inject
  Navigator navigator;

  @Inject
  CredentialDetailFragment(ViewModelProvider.Factory factory) {
    this.factory = factory;
  }

  @Override
  protected int getViewId() {
    return R.layout.fragment_credential_detail;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    setHasOptionsMenu(true);
    // configure the appbar title, FIXME for now it is hardcode as "University Degree"
    return new StackedAppBar(R.string.university_degree);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    MenuItem shareCredentialMenuItem;
    // FIXME missing layout
    /* shareCredentialMenuItem = menu.findItem(R.id.action_share_credential);
    shareCredentialMenuItem.setVisible(true); */
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    // FIXME missing layout
   /* if (item.getItemId() == R.id.action_share_credential) {
      navigator.showFragmentOnTopOfMenu(
          Objects.requireNonNull(getActivity()).getSupportFragmentManager(),
          new ShareCredentialDialogFragment());
      return true;
    }
    if (item.getItemId() == android.R.id.home) {
      Objects.requireNonNull(getActivity()).onBackPressed();
      return true;
    } */
    return super.onOptionsItemSelected(item);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    try {
      viewModel.getCredential(credentialId).observe(this, credential -> {
        textViewUniversityName.setText(credential.getIssuerInfo().getName());
        textViewFullName.setText(credential.getSubject());
        textViewCredentialName.setText(credential.getTitle());
        // TODO add missing fields
        showOptions(credentialIsNew);
      });
    } catch (InvalidProtocolBufferException | InterruptedException | ExecutionException e) {
      Crashlytics.logException(e);
      // TODO show error msg
    }
    return view;
  }

  private void showOptions(boolean optionsVisible) {
    declineButton.setVisibility(optionsVisible ? View.VISIBLE : View.GONE);
    acceptButton.setVisibility(optionsVisible ? View.VISIBLE : View.GONE);
  }

  @OnClick(R.id.accept_credential)
  void onAcceptClick() {
    saveAndGoBack(Preferences.ACCEPTED_MESSAGES_KEY);
  }


  @OnClick(R.id.decline_credential)
  void onDeclineClick() {
    saveAndGoBack(Preferences.REJECTED_MESSAGES_KEY);
  }

  private void saveAndGoBack(String key) {
    Preferences prefs = new Preferences(getContext());
    prefs.saveMessage(credentialId, key);
    getActivity().onBackPressed();
  }

  @Override
  public CredentialsViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(CredentialsViewModel.class);
  }
}

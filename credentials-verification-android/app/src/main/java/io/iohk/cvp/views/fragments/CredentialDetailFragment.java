package io.iohk.cvp.views.fragments;

import static io.iohk.cvp.utils.IntentDataConstants.CREDENTIAL_DATA_KEY;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.android.material.button.MaterialButton;
import io.iohk.cvp.R;
import io.iohk.cvp.io.credential.Credential;
import io.iohk.cvp.io.credential.SentCredential;
import io.iohk.cvp.io.credential.SubjectData;
import io.iohk.cvp.utils.DateUtils;
import io.iohk.cvp.utils.ImageUtils;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.StackedAppBar;
import java.text.MessageFormat;
import java.util.Objects;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class CredentialDetailFragment extends CvpFragment<CredentialsViewModel> {

  private ViewModelProvider.Factory factory;

  @Setter
  private SentCredential credential;

  @Setter
  private String connectionId;

  @Setter
  private String messageId;

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

  @BindView(R.id.text_view_start_date)
  TextView textViewStartDate;

  @BindView(R.id.text_view_graduation_date)
  TextView textViewGraduationDate;

  @BindView(R.id.text_view_award)
  TextView textViewAward;

  @BindView(R.id.credential_logo)
  ImageView imageViewCredentialLogo;

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
    if (!credentialIsNew) {
      shareCredentialMenuItem = menu.findItem(R.id.action_share_credential);
      shareCredentialMenuItem.setVisible(true);
    }
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == R.id.action_share_credential) {
      navigator.showFragmentOnTopOfMenu(
          Objects.requireNonNull(getActivity()).getSupportFragmentManager(), getShareFragment());
      return true;
    }
    if (item.getItemId() == android.R.id.home) {
      Objects.requireNonNull(getActivity()).onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private ShareCredentialDialogFragment getShareFragment() {
    ShareCredentialDialogFragment fragment = new ShareCredentialDialogFragment();
    Bundle args = new Bundle();
    args.putByteArray(CREDENTIAL_DATA_KEY,
        credential.getIssuerSentCredential().getCredential().toByteArray());
    fragment.setArguments(args);

    return fragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    fillData(credential.getIssuerSentCredential().getCredential(), connectionId);
    showOptions(credentialIsNew);

    return view;
  }

  private void fillData(Credential credential, String connectionId) {
    textViewUniversityName.setText(credential.getIssuerType().getIssuerLegalName());

    SubjectData subjectData = credential.getSubjectData();
    textViewFullName.setText(
        MessageFormat.format("{0} {1}", subjectData.getNames(0), subjectData.getSurname(0)));

    textViewCredentialName.setText(credential.getGrantingDecision());

    DateUtils dateUtils = new DateUtils(getContext());

    textViewStartDate.setText(dateUtils.format(credential.getAdmissionDate()));
    textViewGraduationDate.setText(dateUtils.format(credential.getGraduationDate()));

    textViewAward.setText(
        credential.getAdditionalSpeciality() != null ? credential.getDegreeAwarded() : "-");

    Preferences prefs = new Preferences(getContext());

    imageViewCredentialLogo.setImageBitmap(
        ImageUtils.getBitmapFromByteArray(prefs.getConnectionLogo(connectionId)));
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
    prefs.saveMessage(messageId, key);
    getActivity().onBackPressed();
  }

  @Override
  public CredentialsViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(CredentialsViewModel.class);
  }
}

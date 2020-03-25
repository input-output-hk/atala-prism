package io.iohk.cvp.views.fragments;

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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.button.MaterialButton;

import java.text.MessageFormat;
import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.DateUtils;
import io.iohk.cvp.utils.ImageUtils;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.StackedAppBar;
import io.iohk.prism.protos.AlphaCredential;
import io.iohk.prism.protos.AtalaMessage;
import io.iohk.prism.protos.SubjectData;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static io.iohk.cvp.utils.IntentDataConstants.CREDENTIAL_DATA_KEY;

@Setter
@NoArgsConstructor
public class CredentialDetailFragment extends CvpFragment<CredentialsViewModel> {

  private ViewModelProvider.Factory factory;

  @Setter
  private AtalaMessage credential;

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

  @BindView(R.id.text_view_credential_type)
  TextView textViewCredentialType;

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

  @BindView(R.id.credential_logo)
  ImageView imageViewCredentialLogo;

  @BindView(R.id.government_constraint)
  ConstraintLayout governmentConstraint;

  @BindView(R.id.university_constraint)
  ConstraintLayout universityConstraint;

  @BindView(R.id.layout_credential_title)
  ConstraintLayout layoutcredentialtitle;

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
    return new StackedAppBar(R.string.education);
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
    fillData(credential.getIssuerSentCredential().getAlphaCredential(), connectionId);
    showOptions(credentialIsNew);

    return view;
  }

  private void fillData(AlphaCredential credential, String connectionId) {
    //TODO: hardcoded government credential
    if(!connectionId.equals("")) {
      textViewUniversityName.setText(credential.getIssuerType().getIssuerLegalName());

      SubjectData subjectData = credential.getSubjectData();
      textViewFullName.setText(
              MessageFormat.format("{0} {1}", subjectData.getNames(0), subjectData.getSurnames(0)));

      textViewCredentialName.setText(credential.getDegreeAwarded());

      DateUtils dateUtils = new DateUtils(getContext());

      textViewStartDate.setText(dateUtils.format(credential.getAdmissionDate()));
      textViewGraduationDate.setText(dateUtils.format(credential.getGraduationDate()));

      Preferences prefs = new Preferences(getContext());

      imageViewCredentialLogo.setImageBitmap(
              ImageUtils.getBitmapFromByteArray(prefs.getConnectionLogo(connectionId)));
    }else{
      governmentConstraint.setVisibility(View.VISIBLE);
      universityConstraint.setVisibility(View.GONE);

      layoutcredentialtitle.setBackground(getResources().getDrawable(R.drawable.rounded_top_corners_grey));

      textViewCredentialType.setText("National ID Card");
      textViewCredentialType.setTextColor(getResources().getColor(R.color.grey_4));

      textViewCredentialName.setText("Republic of Redland");
      textViewCredentialName.setTextColor(getResources().getColor(R.color.black));

      imageViewCredentialLogo.setImageDrawable(getResources().getDrawable(R.drawable.government_icon));

    }
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
    //TODO: hardcoded government key
    if(key.equals("")){
      getActivity().onBackPressed();
    }

    Preferences prefs = new Preferences(getContext());
    prefs.saveMessage(messageId, key);
    getActivity().onBackPressed();
  }

  @Override
  public CredentialsViewModel getViewModel() {
    CredentialsViewModel viewModel = ViewModelProviders.of(this, factory)
        .get(CredentialsViewModel.class);
    viewModel.setContext(getContext());
    return viewModel;
  }
}

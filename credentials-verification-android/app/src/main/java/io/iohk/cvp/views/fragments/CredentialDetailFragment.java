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

import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.core.enums.CredentialType;
import io.iohk.cvp.utils.CredentialParse;
import io.iohk.cvp.utils.FirebaseAnalyticsEvents;
import io.iohk.cvp.utils.IntentDataConstants;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.viewmodel.dtos.CredentialDto;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.StackedAppBar;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import io.iohk.prism.protos.Credential;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class CredentialDetailFragment extends CvpFragment<CredentialsViewModel> {

    private ViewModelProvider.Factory factory;

    @Setter
    private Credential credential;

    private CredentialDto credentialDto;

    @Setter
    private String connectionId;

    @Setter
    private String messageId;

    @Setter
    private Boolean credentialIsNew;

    @BindView(R.id.text_view_credential_type)
    TextView textViewCredentialType;

    @BindView(R.id.text_view_university_name)
    TextView textViewUniversityName;

    @BindView(R.id.text_view_full_name)
    TextView textViewFullName;

    @BindView(R.id.text_view_credential_name)
    TextView textViewCredentialName;

    @BindView(R.id.text_view_graduation_date_title)
    TextView viewGraduationDateTitle;

    @BindView(R.id.text_view_graduation_date)
    TextView textViewGraduationDate;

    @BindView(R.id.credential_logo)
    ImageView imageViewCredentialLogo;

    @BindView(R.id.goventment_constraint)
    ConstraintLayout goventmentConstraint;

    @BindView(R.id.university_constraint)
    ConstraintLayout universityConstraint;

    @BindView(R.id.layout_credential_title)
    ConstraintLayout layoutcredentialtitle;

    @BindView(R.id.identityValue)
    TextView identityValue;

    @BindView(R.id.birthValue)
    TextView birthValue;

    @BindView(R.id.nameValue)
    TextView nameValue;

    @BindView(R.id.expirationValue)
    TextView expirationValue;

    @BindView(R.id.layout_dates)
    ConstraintLayout layoutDates;

    @BindView(R.id.text_view_award_title)
    TextView awardTitle;

    @BindView(R.id.text_view_award)
    TextView awardValue;

    @BindView(R.id.layout_finish_dates)
    ConstraintLayout layoutFinishDates;

    @BindView(R.id.text_view_university_name_title)
    TextView universityNameTitle;

    @BindView(R.id.text_view_full_name_title)
    TextView fullNameTitle;

    @BindView(R.id.layout_university_name)
    ConstraintLayout layoutUniversityName;

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

        if (credentialDto != null) {
            return new StackedAppBar(credentialDto.getTitle());
        }
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
        args.putByteArray(IntentDataConstants.CREDENTIAL_DATA_KEY, credential.toByteArray());
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        fillData(credential, connectionId);
        saveCredential();
        return view;
    }

    private void fillData(Credential credential, String connectionId) {

        credentialDto = CredentialParse.parse(credential);
        if (credential.getTypeId().equals(CredentialType.REDLAND_CREDENTIAL.getValue())) {

            goventmentConstraint.setVisibility(View.VISIBLE);
            universityConstraint.setVisibility(View.GONE);

            layoutcredentialtitle.setBackground(getResources().getDrawable(R.drawable.rounded_top_corners_grey));

            textViewCredentialType.setText(getResources().getString(R.string.credential_government_name));
            textViewCredentialType.setTextColor(getResources().getColor(R.color.grey_4));

            textViewCredentialName.setText(credentialDto.getIssuer().getName());
            textViewCredentialName.setTextColor(getResources().getColor(R.color.black));

            imageViewCredentialLogo.setImageDrawable(getResources().getDrawable(R.drawable.ic_republic_of_redland));
            identityValue.setText(credentialDto.getCredentialSubject().getIdentityNumber());
            birthValue.setText(credentialDto.getCredentialSubject().getDateOfBirth());
            nameValue.setText(credentialDto.getCredentialSubject().getName());
            expirationValue.setText(credentialDto.getExpiryDate());

        } else {

            if (credential.getTypeId().equals(CredentialType.DEGREE_CREDENTIAL.getValue())) {

                textViewCredentialType.setText(getResources().getString(R.string.university_name));
                textViewCredentialName.setText(credentialDto.getIssuer().getName());
                textViewCredentialName.setTextColor(getResources().getColor(R.color.white));

                universityNameTitle.setText(getResources().getString(R.string.full_name));
                textViewUniversityName.setText(credentialDto.getCredentialSubject().getName());

                awardTitle.setText(getResources().getString(R.string.degree_name));
                awardValue.setText(credentialDto.getCredentialSubject().getDegreeAwarded());

                fullNameTitle.setText(getResources().getString(R.string.award));
                textViewFullName.setText(credentialDto.getCredentialSubject().getDegreeResult());

                viewGraduationDateTitle.setText(getResources().getString(R.string.issuance_date));
                textViewGraduationDate.setText(credentialDto.getIssuanceDate());

                layoutUniversityName.setVisibility(View.VISIBLE);

            } else if (credential.getTypeId().equals(CredentialType.EMPLOYMENT_CREDENTIAL.getValue())) {

                textViewCredentialType.setText(getResources().getString(R.string.company_name));
                textViewCredentialType.setTextColor(getResources().getColor(R.color.white));


                awardTitle.setText(getResources().getString(R.string.employee_name));
                awardValue.setText(credentialDto.getCredentialSubject().getName());

                textViewCredentialName.setText(credentialDto.getIssuer().getName());
                textViewCredentialName.setTextColor(getResources().getColor(R.color.white));

                fullNameTitle.setText(getResources().getString(R.string.employment_status));
                textViewFullName.setText(credentialDto.getEmploymentStatus());

                viewGraduationDateTitle.setText(getResources().getString(R.string.employment_start_date));
                textViewGraduationDate.setText(credentialDto.getIssuanceDate());

                layoutcredentialtitle.setBackground(getResources().getDrawable(R.drawable.rounded_top_corners_purple));
                universityConstraint.setBackground(getResources().getDrawable(R.drawable.rounded_bottom_corners_purple));

                imageViewCredentialLogo.setImageDrawable(getResources().getDrawable(R.drawable.ic_id_proof));

                layoutUniversityName.setVisibility(View.GONE);

            } else {
                //VerifiableCredential/CertificateOfInsurance
                textViewCredentialType.setText(getResources().getString(R.string.provider_name));
                textViewCredentialType.setTextColor(getResources().getColor(R.color.white));

                textViewCredentialName.setText(credentialDto.getIssuer().getName());
                textViewCredentialName.setTextColor(getResources().getColor(R.color.white));


                universityNameTitle.setText(getResources().getString(R.string.full_name));
                textViewUniversityName.setText(credentialDto.getCredentialSubject().getName());

                awardTitle.setText(getResources().getString(R.string.insurance_class));
                awardValue.setText(credentialDto.getProductClass());

                fullNameTitle.setText(getResources().getString(R.string.insurance_number));
                textViewFullName.setText(credentialDto.getPolicyNumber());

                viewGraduationDateTitle.setText(getResources().getString(R.string.insurance_end_date));
                textViewGraduationDate.setText(credentialDto.getExpiryDate());

                layoutcredentialtitle.setBackground(getResources().getDrawable(R.drawable.rounded_top_corners_blue));
                universityConstraint.setBackground(getResources().getDrawable(R.drawable.rounded_bottom_corners_white));

                imageViewCredentialLogo.setImageDrawable(getResources().getDrawable(R.drawable.ic_insurance_detail));

                textViewUniversityName.setTextColor(getResources().getColor(R.color.black));
                universityNameTitle.setTextColor(getResources().getColor(R.color.black));
                awardTitle.setTextColor(getResources().getColor(R.color.black));
                awardValue.setTextColor(getResources().getColor(R.color.black));
                fullNameTitle.setTextColor(getResources().getColor(R.color.black));
                textViewFullName.setTextColor(getResources().getColor(R.color.black));
                viewGraduationDateTitle.setTextColor(getResources().getColor(R.color.black));
                textViewGraduationDate.setTextColor(getResources().getColor(R.color.black));

            }

        }
    }

    private void saveCredential() {
        Preferences pref = new Preferences(getContext());

        Set<String> acceptedMessagesIds = pref.getStoredMessages(Preferences.ACCEPTED_MESSAGES_KEY);
        if(!acceptedMessagesIds.contains(messageId)){
            pref.saveMessage(messageId, Preferences.ACCEPTED_MESSAGES_KEY);
        }
    }

    @Override
    public CredentialsViewModel getViewModel() {
        CredentialsViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(CredentialsViewModel.class);
        viewModel.setContext(getContext());
        return viewModel;
    }
}
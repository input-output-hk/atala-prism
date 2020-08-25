package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.core.enums.CredentialType;
import io.iohk.cvp.data.local.db.model.Credential;
import io.iohk.cvp.utils.CredentialParse;
import io.iohk.cvp.utils.IntentDataConstants;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.viewmodel.dtos.CredentialDto;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.StackedAppBar;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class CredentialDetailFragment extends CvpFragment<CredentialsViewModel> implements DeleteCredentialDialogFragment.OnDeleteCredential {

    @Inject
    ViewModelProvider.Factory factory;

    private static final int DELETE_ALL_CONNECTIONS_REQUEST_CODE = 22;

    @Setter
    private Credential credential;

    private CredentialDto credentialDto;

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
        MenuItem deleteCredentialMenuItem = menu.findItem(R.id.action_delete_credential);
        deleteCredentialMenuItem.setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_share_credential:
                navigator.showFragmentOnTopOfMenu(
                        requireActivity().getSupportFragmentManager(), getShareFragment());
                return true;
            case android.R.id.home:
                requireActivity().onBackPressed();
                return true;
            case R.id.action_delete_credential:
                navigator.showDialogFragment(
                        requireActivity().getSupportFragmentManager(), getDeleteCredentialFragment(), null);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ShareCredentialDialogFragment getShareFragment() {
        ShareCredentialDialogFragment fragment = new ShareCredentialDialogFragment();
        Bundle args = new Bundle();
        args.putString(IntentDataConstants.CREDENTIAL_DATA_KEY, credential.credentialDocument);
        args.putString(IntentDataConstants.CREDENTIAL_TYPE_KEY, credential.credentialType);
        args.putByteArray(IntentDataConstants.CREDENTIAL_ENCODED_KEY, credential.credentialEncoded.toByteArray());
        fragment.setArguments(args);

        return fragment;
    }

    private DeleteCredentialDialogFragment getDeleteCredentialFragment() {
        DeleteCredentialDialogFragment fragment = new DeleteCredentialDialogFragment();
        fragment.setTargetFragment(this, DELETE_ALL_CONNECTIONS_REQUEST_CODE);
        Bundle args = new Bundle();
        args.putString(IntentDataConstants.CREDENTIAL_TYPE_KEY, credential.credentialType);
        args.putString(IntentDataConstants.CREDENTIAL_ID_KEY, credential.credentialId);
        args.putString(IntentDataConstants.CREDENTIAL_DATA_KEY, credential.credentialDocument);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        fillData(credential);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getViewModel().setCredentialViewed(credential);
    }

    private void fillData(Credential credential) {

        String credentialType = credential.credentialType;
        credentialDto = CredentialParse.parse(credential.credentialType, credential.credentialDocument);
        if (credentialType.equals(CredentialType.REDLAND_CREDENTIAL.getValue())) {

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

            if (credentialType.equals(CredentialType.DEGREE_CREDENTIAL.getValue())) {

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

            } else if (credentialType.equals(CredentialType.EMPLOYMENT_CREDENTIAL.getValue())) {

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

    @Override
    public CredentialsViewModel getViewModel() {
        CredentialsViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(CredentialsViewModel.class);
        return viewModel;
    }

    @Override
    public void credentialDeleted() {
        getFragmentManager().popBackStack();
    }
}
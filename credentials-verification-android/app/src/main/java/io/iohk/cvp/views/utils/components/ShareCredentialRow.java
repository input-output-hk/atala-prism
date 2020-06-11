package io.iohk.cvp.views.utils.components;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.crashlytics.android.Crashlytics;

import java.util.Optional;
import java.util.function.Consumer;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;
import io.iohk.cvp.core.enums.CredentialType;
import io.iohk.cvp.utils.CredentialParse;
import io.iohk.cvp.viewmodel.dtos.CredentialDto;
import io.iohk.cvp.views.fragments.ShareProofRequestDialogFragment;
import io.iohk.prism.protos.Credential;

public class ShareCredentialRow extends ConstraintLayout {

    @BindView(R.id.name)
    public TextView name;

    @BindView(R.id.credential_checkbox)
    public CheckBox credentialCheckbox;

    private ShareProofRequestDialogFragment fragment;
    private Credential credential;

    public ShareCredentialRow(ShareProofRequestDialogFragment fragment, Credential credential) {
        super(fragment.getActivity());
        this.credential = credential;
        this.fragment = fragment;
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.row_share_credential, this);
        ButterKnife.bind(this);
        name.setText(credentialTittle(credential));

        credentialCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fragment.enableShareButton();
            }
        });
    }

    public boolean isChecked(){
        return credentialCheckbox.isChecked();
    }

    private String credentialTittle(Credential credential){
        Optional<CredentialType> credentialTypeOptional = CredentialType.getByValue(credential.getTypeId());
        if(credentialTypeOptional.isPresent()) {
            switch (credentialTypeOptional.get().getId()) {
                case 1:
                    return getResources().getString(R.string.credential_detail_government_title);
                case 2:
                    return getResources().getString(R.string.credential_detail_degree_title);
                case 3:
                    return getResources().getString(R.string.credential_detail_employed_title);
                case 4:
                    return getResources().getString(R.string.credential_detail_insurance_title);
                default:
                    return "";
            }
        }
        return "";
    }
}

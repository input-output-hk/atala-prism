package io.iohk.cvp.views.utils.components;

import android.content.Context;
import android.widget.CheckBox;
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
import io.iohk.prism.protos.Credential;

public class ShareCredentialRow extends ConstraintLayout {

    @BindView(R.id.name)
    public TextView name;

    @BindView(R.id.credential_checkbox)
    public CheckBox credentialCheckbox;

    private Credential credential;

    public ShareCredentialRow(@NonNull Context context, Credential credential) {
        super(context);
        this.credential = credential;
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.row_share_credential, this);
        ButterKnife.bind(this);
        name.setText(credentialTittle(credential));
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

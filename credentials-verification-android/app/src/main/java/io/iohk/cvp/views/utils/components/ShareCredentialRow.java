package io.iohk.cvp.views.utils.components;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;
import io.iohk.cvp.data.local.db.model.Credential;
import io.iohk.cvp.views.fragments.CredentialUtil;
import io.iohk.cvp.views.fragments.ShareProofRequestDialogFragment;

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
        name.setText(CredentialUtil.getTitle(credential, fragment.getContext()));

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

}

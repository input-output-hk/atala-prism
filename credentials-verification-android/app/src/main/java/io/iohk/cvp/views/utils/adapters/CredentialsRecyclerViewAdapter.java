package io.iohk.cvp.views.utils.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.core.enums.CredentialType;
import io.iohk.cvp.utils.CredentialParse;
import io.iohk.cvp.viewmodel.dtos.CredentialDto;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.prism.protos.AtalaMessage;
import io.iohk.prism.protos.Credential;
import io.iohk.prism.protos.ReceivedMessage;

public class CredentialsRecyclerViewAdapter extends
        RecyclerView.Adapter<CredentialsRecyclerViewAdapter.ViewHolder> {

    private final int holderLayoutId;
    private final HomeFragment listener;
    private final Boolean hasNewCredentials;
    private final Preferences preferences;

    private List<ReceivedMessage> messages = new ArrayList<>();

    public CredentialsRecyclerViewAdapter(int holderLayoutId, HomeFragment listener,
                                          Boolean hasNewCredentials, Preferences prefs) {
        this.holderLayoutId = holderLayoutId;
        this.listener = listener;
        this.hasNewCredentials = hasNewCredentials;
        this.preferences = prefs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(holderLayoutId, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(CredentialsRecyclerViewAdapter.ViewHolder holder, int position) {
        try {
            ReceivedMessage msg = messages.get(position);
            Credential current = AtalaMessage.parseFrom(msg.getMessage()).getIssuerSentCredential().getCredential();
            CredentialDto credentialDto = CredentialParse.parse(current);

            holder.credential = current;
            holder.messageId = msg.getId();
            holder.connectionId = msg.getConnectionId();
            holder.listener = this.listener;
            holder.isNew = this.hasNewCredentials;

            if (current.getTypeId().equals(CredentialType.REDLAND_CREDENTIAL.getValue())) {
                holder.credentialType.setText(listener.getResources().getString(R.string.credential_government_name));
                holder.issuerLogo.setImageDrawable(listener.getResources().getDrawable(R.drawable.ic_id_government));
            } else if (current.getTypeId().equals(CredentialType.DEGREE_CREDENTIAL.getValue())) {
                holder.credentialType.setText(listener.getResources().getString(R.string.credential_degree_name));
                holder.issuerLogo.setImageDrawable(listener.getResources().getDrawable(R.drawable.ic_id_university));
            } else if (current.getTypeId().equals(CredentialType.EMPLOYMENT_CREDENTIAL.getValue())) {
                holder.credentialType.setText(listener.getResources().getString(R.string.credential_employment_name));
                holder.issuerLogo.setImageDrawable(listener.getResources().getDrawable(R.drawable.ic_id_proof));
            } else {
                //Certificate Of Insurance
                holder.credentialType.setText(listener.getResources().getString(R.string.credential_insurance_name));
                holder.issuerLogo.setImageDrawable(listener.getResources().getDrawable(R.drawable.ic_id_insurance));
            }
            holder.credentialName.setText(credentialDto.getIssuer().getName());

        } catch (Exception e) {
            Crashlytics.logException(e);
        }
    }

    public void addMesseges(List<ReceivedMessage> newMessages) {
        if(!this.messages.containsAll(newMessages)){
            this.messages.addAll(newMessages);
            this.notifyDataSetChanged();
        }
    }

    public void clearMessages() {
        messages = new ArrayList<>();
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        String connectionId;
        Credential credential;
        String messageId;

        @BindView(R.id.credential_type)
        TextView credentialType;

        @BindView(R.id.credential_name)
        TextView credentialName;

        @BindView(R.id.credential_logo)
        ImageView issuerLogo;

        HomeFragment listener;
        Boolean isNew;


        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.issuer_card_view)
        public void onCredentialClicked() {
            listener.onCredentialClicked(isNew, credential, connectionId, messageId);
        }

    }
}
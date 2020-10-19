package io.iohk.atala.prism.app.views.utils.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.core.enums.CredentialType;
import io.iohk.atala.prism.app.data.local.db.model.Credential;

public class NewCredentialsRecyclerViewAdapter extends
        RecyclerView.Adapter<NewCredentialsRecyclerViewAdapter.ViewHolder> {

    private final int holderLayoutId;
    private final CredentialClickListener listener;
    private final Boolean hasNewCredentials;
    private final Context context;

    private List<Credential> credentials = new ArrayList<>();

    public NewCredentialsRecyclerViewAdapter(int holderLayoutId, CredentialClickListener listener,
                                             Boolean hasNewCredentials, Context context) {
        this.holderLayoutId = holderLayoutId;
        this.listener = listener;
        this.hasNewCredentials = hasNewCredentials;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(holderLayoutId, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(NewCredentialsRecyclerViewAdapter.ViewHolder holder, int position) {
        try {
            Credential credential = credentials.get(position);
            holder.credential = credential;
            holder.listener = this.listener;
            holder.isNewCredential = hasNewCredentials;
            String credentialType = credential.credentialType;

            holder.credentialType.setText(credentialType);

            if (credentialType.equals(CredentialType.REDLAND_CREDENTIAL.getValue())) {
                holder.credentialType.setText(context.getResources().getString(R.string.credential_government_name));
                holder.issuerLogo.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_id_government));
            } else if (credentialType.equals(CredentialType.DEGREE_CREDENTIAL.getValue())) {
                holder.credentialType.setText(context.getResources().getString(R.string.credential_degree_name));
                holder.issuerLogo.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_id_university));
            } else if (credentialType.equals(CredentialType.EMPLOYMENT_CREDENTIAL.getValue())) {
                holder.credentialType.setText(context.getResources().getString(R.string.credential_employment_name));
                holder.issuerLogo.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_id_proof));
            } else {
                //Certificate Of Insurance
                holder.credentialType.setText(context.getResources().getString(R.string.credential_insurance_name));
                holder.issuerLogo.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_id_insurance));
            }
            holder.credentialName.setText(credential.issuerName);

        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public void addMesseges(List<Credential> newCredentialsList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CredentialDiffCallback(newCredentialsList, credentials));
        credentials.clear();
        this.credentials.addAll(newCredentialsList);
        diffResult.dispatchUpdatesTo(this);
    }

    public void clearMessages() {
        credentials = new ArrayList<>();
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return credentials.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        Credential credential;

        Boolean isNewCredential;

        @BindView(R.id.credential_type)
        TextView credentialType;

        @BindView(R.id.credential_name)
        TextView credentialName;

        @BindView(R.id.credential_logo)
        ImageView issuerLogo;

        CredentialClickListener listener;


        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.issuer_card_view)
        public void onCredentialClicked() {
            listener.onCredentialClickListener(isNewCredential, credential);
        }

    }

}
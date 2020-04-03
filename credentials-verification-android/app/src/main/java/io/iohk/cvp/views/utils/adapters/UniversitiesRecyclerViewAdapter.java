package io.iohk.cvp.views.utils.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.IssuerInfo;

public class UniversitiesRecyclerViewAdapter extends
        ConnectionsRecyclerViewAdapter<UniversitiesRecyclerViewAdapter.ViewHolder> {

    @Override
    public void onBindViewHolder(UniversitiesRecyclerViewAdapter.ViewHolder holder,
                                 ConnectionInfo connectionInfo) {
        IssuerInfo issuerInfo = connectionInfo.getParticipantInfo().getIssuer();
        holder.issuerName.setText(issuerInfo.getName());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.row_university_connection_list;
    }

    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.issuer_name)
        TextView issuerName;

        @BindView(R.id.credential_logo)
        ImageView issuerLogo;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}


package io.iohk.atala.prism.app.views.utils.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.utils.ImageUtils;
import io.iohk.atala.prism.protos.ConnectionInfo;
import io.iohk.atala.prism.protos.VerifierInfo;

public class EmployersRecyclerViewAdapter extends
        ConnectionsRecyclerViewAdapter<EmployersRecyclerViewAdapter.ViewHolder> {

    @Override
    protected int getLayoutId() {
        return R.layout.row_employer_connection_list;
    }

    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(ViewHolder holder, ConnectionInfo connectionInfo) {
        VerifierInfo verifierInfo = connectionInfo.getParticipantInfo().getVerifier();
        holder.verifierName.setText(verifierInfo.getName());
        holder.verifierLogo
                .setImageBitmap(ImageUtils.getBitmapFromByteArray(verifierInfo.getLogo().toByteArray()));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.issuer_name)
        TextView verifierName;

        @BindView(R.id.credential_logo)
        ImageView verifierLogo;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
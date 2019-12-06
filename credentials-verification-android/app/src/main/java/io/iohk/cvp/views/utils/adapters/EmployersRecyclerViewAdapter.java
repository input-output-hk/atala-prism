package io.iohk.cvp.views.utils.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;
import io.iohk.cvp.io.connector.ConnectionInfo;
import io.iohk.cvp.io.connector.VerifierInfo;
import io.iohk.cvp.utils.ImageUtils;

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


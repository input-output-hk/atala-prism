package io.iohk.cvp.views.utils.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;
import io.iohk.cvp.io.connector.ConnectionInfo;

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
    // TODO unmock this when we are sure about which class's info are we going to show here
    holder.issuerName.setText("Business and Technology University");
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


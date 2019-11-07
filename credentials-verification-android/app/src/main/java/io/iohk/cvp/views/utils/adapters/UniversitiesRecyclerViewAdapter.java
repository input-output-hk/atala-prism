package io.iohk.cvp.views.utils.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;
import io.iohk.cvp.io.connector.ConnectionInfo;
import io.iohk.cvp.io.connector.IssuerInfo;
import java.util.List;

public class UniversitiesRecyclerViewAdapter extends
    ConnectionsRecyclerViewAdapter<UniversitiesRecyclerViewAdapter.ViewHolder> {

  private List<ConnectionInfo> connections;

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

    View v = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.row_university_connection_list, parent, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(UniversitiesRecyclerViewAdapter.ViewHolder holder, int position) {
    IssuerInfo issuerInfo = connections.get(position).getParticipantInfo().getIssuer();
    holder.issuerName.setText(issuerInfo.getName());
  }

  @Override
  public int getItemCount() {
    if (connections != null) {
      return connections.size();
    } else {
      return 0;
    }
  }

  public void setConnections(List<ConnectionInfo> connections) {
    this.connections = connections;
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


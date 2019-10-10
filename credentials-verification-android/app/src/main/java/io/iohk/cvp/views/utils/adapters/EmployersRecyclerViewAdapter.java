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
import java.util.List;

public class EmployersRecyclerViewAdapter extends
    ConnectionsRecyclerViewAdapter<EmployersRecyclerViewAdapter.ViewHolder> {

  private List<ConnectionInfo> connections;

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

    View v = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.row_employer_connection_list, parent, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(EmployersRecyclerViewAdapter.ViewHolder holder, int position) {
    // TODO unmock this when we are sure about which class's info are we going to show here
    holder.issuerName.setText("Business and Technology University");
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

    @BindView(R.id.issuer_logo)
    ImageView issuerLogo;


    ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

  }
}


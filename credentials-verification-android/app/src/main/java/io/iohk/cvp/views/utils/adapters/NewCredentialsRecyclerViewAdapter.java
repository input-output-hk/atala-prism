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
import io.iohk.cvp.io.connector.Credential;
import java.util.ArrayList;
import java.util.List;

public class NewCredentialsRecyclerViewAdapter extends
    RecyclerView.Adapter<NewCredentialsRecyclerViewAdapter.ViewHolder> {

  private List<Credential> credentials = new ArrayList<>();

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.row_new_credential, parent, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(NewCredentialsRecyclerViewAdapter.ViewHolder holder, int position) {
    // TODO unmock this when we are sure about which class's info are we going to show here
    holder.issuerName.setText("Business and Technology University");
  }

  @Override
  public int getItemCount() {
    return credentials.size();
  }

  public void setCredentials(List<Credential> credentials) {
    this.credentials = credentials;
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


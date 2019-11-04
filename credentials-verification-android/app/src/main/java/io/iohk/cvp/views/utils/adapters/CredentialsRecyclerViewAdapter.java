package io.iohk.cvp.views.utils.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.io.connector.Credential;
import io.iohk.cvp.views.fragments.HomeFragment;
import lombok.Setter;

public class CredentialsRecyclerViewAdapter extends
    RecyclerView.Adapter<CredentialsRecyclerViewAdapter.ViewHolder> {

  private final int holderLayoutId;
  private final HomeFragment listener;
  private final Boolean hasNewCredentials;

  @Setter
  private List<Credential> credentials = new ArrayList<>();

  public CredentialsRecyclerViewAdapter(int holderLayoutId, HomeFragment listener,
      Boolean hasNewCredentials) {
    this.holderLayoutId = holderLayoutId;
    this.listener = listener;
    this.hasNewCredentials = hasNewCredentials;
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
    // TODO unmock this when we are sure about which class's info are we going to show here
    holder.listener = this.listener;
    holder.isNew = this.hasNewCredentials;
    holder.issuerName.setText("Business and Technology University");
  }

  @Override
  public int getItemCount() {
    return credentials.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.issuer_name)
    TextView issuerName;

    @BindView(R.id.issuer_logo)
    ImageView issuerLogo;

    HomeFragment listener;
    Boolean isNew;


    ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    @OnClick(R.id.issuer_card_view)
    public void onCredentialClicked() {
      listener.onCredentialClicked(isNew ? "newCredential" : "");
    }
  }
}


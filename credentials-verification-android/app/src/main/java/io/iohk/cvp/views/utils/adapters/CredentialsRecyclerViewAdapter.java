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
import butterknife.OnClick;
import com.crashlytics.android.Crashlytics;
import com.google.protobuf.InvalidProtocolBufferException;
import io.iohk.cvp.R;
import io.iohk.cvp.io.connector.ReceivedMessage;
import io.iohk.cvp.io.credential.Credential;
import io.iohk.cvp.io.credential.SentCredential;
import io.iohk.cvp.views.fragments.HomeFragment;
import java.util.ArrayList;
import java.util.List;

public class CredentialsRecyclerViewAdapter extends
    RecyclerView.Adapter<CredentialsRecyclerViewAdapter.ViewHolder> {

  private final int holderLayoutId;
  private final HomeFragment listener;
  private final Boolean hasNewCredentials;

  private List<ReceivedMessage> messages = new ArrayList<>();

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
    try {
      ReceivedMessage msg = messages.get(position);
      SentCredential sentCredential = SentCredential.parseFrom(msg.getMessage());
      Credential current = sentCredential.getIssuerSentCredential().getCredential();
      holder.messageId = msg.getId();
      holder.listener = this.listener;
      holder.isNew = this.hasNewCredentials;
      holder.issuerName.setText(current.getIssuerType().getAcademicAuthority());
      // TODO set image
    } catch (InvalidProtocolBufferException e) {
      Crashlytics.logException(e);
    }
  }

  public void addMesseges(List<ReceivedMessage> newMessages) {
    this.messages.addAll(newMessages);
    this.notifyDataSetChanged();
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

    String messageId;

    @BindView(R.id.issuer_name)
    TextView issuerName;

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
      listener.onCredentialClicked(isNew, messageId);
    }

  }
}


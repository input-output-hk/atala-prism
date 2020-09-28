package io.iohk.cvp.views.utils.adapters;

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
import io.iohk.cvp.R;
import io.iohk.cvp.data.local.db.model.Contact;
import io.iohk.cvp.utils.ImageUtils;
import lombok.Setter;

@Setter
public class ContactsRecyclerViewAdapter extends
        RecyclerView.Adapter<ContactsRecyclerViewAdapter.ContactViewHolder> {

    private final ContactItemInteraction listener;
    protected List<Contact> connections = new ArrayList<>();

    public ContactsRecyclerViewAdapter(ContactItemInteraction contactItemInteraction) {
        listener = contactItemInteraction;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_university_connection_list, parent, false);
        return new ContactViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact connection = connections.get(position);
        holder.issuerName.setText(connection.name);
        byte[] logo = connection.logo;
        if (logo != null && logo.length > 0) {
            try {
                holder.issuerLogo.setImageBitmap(ImageUtils.getBitmapFromByteArray(logo));
            } catch (Exception ex) {
                FirebaseCrashlytics.getInstance().recordException(ex);
            }
        }
        holder.deleteContactBtn.setOnClickListener(v -> {
            listener.onDeleteClicked(connection);
        });
    }

    @Override
    public int getItemCount() {
        return connections.size();
    }

    public void addConnections(List<Contact> newConnections) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ContactsDiffCallback(newConnections, connections));
        connections.clear();
        this.connections.addAll(newConnections);
        diffResult.dispatchUpdatesTo(this);
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.issuer_name)
        TextView issuerName;

        @BindView(R.id.credential_logo)
        ImageView issuerLogo;

        @BindView(R.id.delete_contact_btn)
        ImageView deleteContactBtn;

        ContactViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface ContactItemInteraction {
        void onDeleteClicked(Contact connection);
    }
}